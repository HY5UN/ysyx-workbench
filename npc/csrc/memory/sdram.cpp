#include "include/common.h"
#include "include/config.h"
#include "include/CPU.h"
#include "include/trace.h"
#include <fstream>
#include <chrono>
#include <algorithm>

#define SDRAM_SIZE (32 * 1024 * 1024 * 2 * 2) // 128 MB
uint16_t sdram[SDRAM_SIZE / 2];

/* ----------------------------------------------------------------
 * 访问覆盖率统计（仅用于仿真进度展示，不影响功能逻辑）
 *
 * 把整个 128MB 空间分成 1024*128 = 131072 份，每份大小:
 *   128MB / 131072 = 1024 字节 = 512 个 16-bit word
 * 即每份覆盖 SDRAM_CHUNK_WORDS 个连续的 addr（word 地址）。
 * 只要这一份内任意一个 addr 被 sdram_read 读取过，就认为这一份
 * “被访问”了；每过一分钟（墙钟时间）打印一次累计访问份数和百分比。
 * ---------------------------------------------------------------- */
#define SDRAM_CHUNK_NUM   (1024 * 128)                          // 总份数: 131072
#define SDRAM_CHUNK_WORDS ((SDRAM_SIZE / 2) / SDRAM_CHUNK_NUM)   // 每份覆盖的 16-bit word 数

static bool sdram_chunk_visited[SDRAM_CHUNK_NUM] = {false};
static uint64_t sdram_visited_chunk_cnt = 0;
static uint64_t sdram_round_cnt = 0;   // 已跑完的轮数（覆盖率集满并清零的次数）
static std::chrono::steady_clock::time_point sdram_progress_last_print = std::chrono::steady_clock::now();

// 标记 addr 所在的份为已访问（只在读操作中调用，写操作不计入）
// 一旦全部份都被访问过（覆盖率集满），说明一轮（字节/半字/字/双字
// 中的某一轮）已经扫完整个空间，自动清零重新开始计数，方便下一轮
// 重新观察进度。
static inline void sdram_mark_chunk_visited(int addr)
{
    uint32_t chunk_idx = (uint32_t)addr / SDRAM_CHUNK_WORDS;
    if (chunk_idx >= SDRAM_CHUNK_NUM)
        return;
    if (!sdram_chunk_visited[chunk_idx])
    {
        sdram_chunk_visited[chunk_idx] = true;
        sdram_visited_chunk_cnt++;

        if (sdram_visited_chunk_cnt == SDRAM_CHUNK_NUM)
        {
            sdram_round_cnt++;
            fprintf(stderr, "[NPC] sdram coverage: round %llu complete (all %d chunks visited), reset coverage\n",
                    (unsigned long long)sdram_round_cnt, SDRAM_CHUNK_NUM);
            std::fill(std::begin(sdram_chunk_visited), std::end(sdram_chunk_visited), false);
            sdram_visited_chunk_cnt = 0;
        }
    }
}

// 距上次打印超过60秒则打印一次当前覆盖进度
// 注意: steady_clock::now() 本身不算贵，但访问次数一旦上到几十亿级别
// 累积开销也不可忽略，所以不是每次调用都真去查时钟，而是每隔
// SDRAM_PROGRESS_CHECK_INTERVAL 次访问才检查一次（粒度本来就是分钟级，
// 稍微滞后几千次访问完全不影响效果）
#define SDRAM_PROGRESS_CHECK_INTERVAL 8192  // 2的幂，取模可优化成位运算

static inline void sdram_progress_tick(void)
{
    static uint64_t call_cnt = 0;
    if ((++call_cnt & (SDRAM_PROGRESS_CHECK_INTERVAL - 1)) != 0)
        return;

    auto now = std::chrono::steady_clock::now();
    auto elapsed_sec = std::chrono::duration_cast<std::chrono::seconds>(now - sdram_progress_last_print).count();
    if (elapsed_sec >= 60)
    {
        double percent = 100.0 * (double)sdram_visited_chunk_cnt / (double)SDRAM_CHUNK_NUM;
        fprintf(stderr, "[NPC] sdram coverage: round %llu: %llu/%d chunks visited (%.2f%%)\n",
                (unsigned long long)(sdram_round_cnt + 1),
                (unsigned long long)sdram_visited_chunk_cnt, SDRAM_CHUNK_NUM, percent);
        sdram_progress_last_print = now;
    }
}

extern "C" void sdram_read(int addr, int16_t *rdata)
{
    if ((uint32_t)addr >= SDRAM_SIZE / 2)
    {
        fprintf(stderr, "[NPC] sdram_read: addr 0x%x out of range\n", addr);
        *rdata = 0;
        return;
    }
    *rdata = sdram[addr];

    sdram_mark_chunk_visited(addr);
    sdram_progress_tick();

#ifdef ENABLE_ITRACE
    char msg[128];
    // sprintf(msg, "[[SDRAM] R addr=0x%08x: 0x%04x | ba=%02x row=%04x col=%03x ]", addr, (uint16_t)*rdata, (addr >> 22) & 0x3, (addr >> 9) & 0x1FFF, addr & 0x1FF);
    sprintf(msg, "[R addr= %02x %04x %03x %01x: 0x%04x]", (addr >> 23) & 0x3, (addr >> 10) & 0x1FFF, (addr >> 1) & 0x1FF,addr & 0x1, (uint16_t)*rdata);
    mtrace_record(msg);
#endif
}

extern "C" void sdram_write(int addr, int16_t wdata, char dqm)
{
#ifdef ENABLE_ITRACE
    char msg[128];
    // sprintf(msg, "[[SDRAM] W addr=0x%08x: 0x%04x dqm=0b%02b | ba=%02x row=%04x col=%03x ]", addr, (uint16_t)wdata, (uint8_t)dqm, (addr >> 22) & 0x3, (addr >> 9) & 0x1FFF, addr & 0x1FF);
    sprintf(msg, "[W addr= %02x %04x %03x %01x: 0x%04x dqm=0b%02b]",  (addr >> 23) & 0x3, (addr >> 10) & 0x1FFF, (addr >> 1) & 0x1FF, addr & 0x1, (uint16_t)wdata, (uint8_t)dqm);
    mtrace_record(msg);
#endif
    if ((uint32_t)addr >= SDRAM_SIZE / 2)
    {
        fprintf(stderr, "[NPC] sdram_write: addr 0x%x out of range\n", addr);
        return;
    }

    // 写操作不计入访问覆盖率，但仍触发一次进度检查，
    // 避免读操作很稀疏时进度打印被无限期推迟
    sdram_progress_tick();

    uint16_t current = sdram[addr];
    uint16_t mask = 0;

    if ((dqm & 0x1) == 0) // dqm[0]=0 写入低字节
        mask |= 0x00FF;
    if ((dqm & 0x2) == 0) // dqm[1]=0 写入高字节
        mask |= 0xFF00;

    sdram[addr] = (current & ~mask) | ((uint16_t)wdata & mask);
}