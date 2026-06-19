#include "include/common.h"
#include "include/config.h"
#include "include/CPU.h"
#include <fstream>

#define SDRAM_SIZE (32 * 1024 * 1024) // 32 MB
uint16_t sdram[SDRAM_SIZE / 2];

extern "C" void sdram_read(int addr, int16_t *rdata)
{
    if ((uint32_t)addr >= SDRAM_SIZE / 2)
    {
        fprintf(stderr, "[NPC] sdram_read: addr 0x%x out of range\n", addr);
        *rdata = 0;
        return;
    }
    *rdata = sdram[addr];
    // printf(".");
    printf("[NPC] sdram_read: addr=0x%08x, data=0x%04x, at cycle=%llu\n", addr, (uint16_t)*rdata, cpu->cycle_count);
}

extern "C" void sdram_write(int addr, int16_t wdata, char dqm)
{
    printf("[NPC] sdram_write: addr=0x%08x, data=0x%04x, dqm=0x%02x, at cycle=%llu\n",
           addr, (uint16_t)wdata, (uint8_t)dqm, cpu->cycle_count);

    if ((uint32_t)addr >= SDRAM_SIZE / 2)
    {
        fprintf(stderr, "[NPC] sdram_write: addr 0x%x out of range\n", addr);
        return;
    }

    // 根据 dqm 低两位选择性写入字节
    uint16_t current = sdram[addr];
    uint16_t mask = 0;

    if ((dqm & 0x1) == 0)   // dqm[0]=0 写入低字节
        mask |= 0x00FF;
    if ((dqm & 0x2) == 0)   // dqm[1]=0 写入高字节
        mask |= 0xFF00;

    sdram[addr] = (current & ~mask) | ((uint16_t)wdata & mask);
}