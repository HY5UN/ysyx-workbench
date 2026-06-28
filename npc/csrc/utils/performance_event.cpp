#include <svdpi.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include "include/common.h"
#include "include/CPU.h"

// ==========================================
// 统计开关配置
// ==========================================
// 开关：决定是否统计 Flash 数据（耗时 > 100 周期的操作）
// - true : 统计全部数据（包含 Flash 长周期）
// - false: 仅统计周期数 <= 100 的短周期数据
const bool INCLUDE_FLASH_DATA = false; 
const uint64_t FLASH_THRESHOLD = 300;

// ==========================================
// 全局状态与计数器定义
// ==========================================

// 1. 各类指令执行周期统计
enum InstType { 
    NONE = 0, R_TYPE, I_TYPE, L_TYPE, S_TYPE, U_TYPE, B_TYPE, J_TYPE, CSR_TYPE, SYS_TYPE, TYPE_COUNT 
};
const char* inst_names[] = {
    "None", "R-Type", "I-Type", "Load", "Store", "U-Type", "B-Type", "J-Type", "CSR", "System"
};

uint64_t inst_cycles[TYPE_COUNT] = {0}; // 记录某种指令到下一次取指花费的总周期
uint64_t inst_counts[TYPE_COUNT] = {0}; // 记录某种指令的总条数

InstType current_inst = NONE;           // 当前正在执行的指令类型
uint64_t current_inst_cycle_counter = 0;// 当前指令正在累加的周期

// 2. 取指 (IF) 周期统计
uint64_t total_if_cycles = 0;
uint64_t total_if_counts = 0;
uint64_t current_if_counter = 0;
bool is_fetching = false;

// --- 新增：取指缓存命中/未命中统计 ---
uint64_t total_if_hit_cycles = 0;
uint64_t total_if_hit_counts = 0;
uint64_t total_if_miss_cycles = 0;
uint64_t total_if_miss_counts = 0;
bool current_if_missed = false; // 用于锁存当前取指请求中是否检测到了 io_if_miss 信号

// 3. LSU 读周期统计
uint64_t total_lsur_cycles = 0;
uint64_t total_lsur_counts = 0;
uint64_t current_lsur_counter = 0;
bool is_lsur = false;

// 4. LSU 写周期统计
uint64_t total_lsuw_cycles = 0;
uint64_t total_lsuw_counts = 0;
uint64_t current_lsuw_counter = 0;
bool is_lsuw = false;


// ==========================================
// DPI-C 周期采样函数 (每周期执行)
// ==========================================
extern "C" void dpic_save_performance_event(
    svBit io_if_begin,
    svBit io_if_miss,
    svBit io_if_finish,
    svBit io_lsu_r_begin,
    svBit io_lsu_r_finish,
    svBit io_lsu_w_begin,   
    svBit io_lsu_w_finish,
    svBit io_exu,
    svBit io_inst_r,
    svBit io_inst_i,
    svBit io_inst_l,
    svBit io_inst_s,
    svBit io_inst_u,
    svBit io_inst_b,
    svBit io_inst_j,
    svBit io_inst_csr,
    svBit io_inst_sys
) {
    // ---------------------------------------------------------
    // 任务1：统计每种指令执行到下一次取指开始的周期数
    // ---------------------------------------------------------
    if (io_if_begin) {
        if (current_inst != NONE) {
            if (INCLUDE_FLASH_DATA || current_inst_cycle_counter <= FLASH_THRESHOLD) {
                inst_cycles[current_inst] += current_inst_cycle_counter;
                inst_counts[current_inst]++;
            }
        }
        current_inst = NONE; 
        current_inst_cycle_counter = 0; 
    } 
    
    current_inst_cycle_counter++;

    if (io_inst_r) current_inst = R_TYPE;
    else if (io_inst_i) current_inst = I_TYPE;
    else if (io_inst_l) current_inst = L_TYPE;
    else if (io_inst_s) current_inst = S_TYPE;
    else if (io_inst_u) current_inst = U_TYPE;
    else if (io_inst_b) current_inst = B_TYPE;
    else if (io_inst_j) current_inst = J_TYPE;
    else if (io_inst_csr) current_inst = CSR_TYPE;
    else if (io_inst_sys) current_inst = SYS_TYPE;

    // ---------------------------------------------------------
    // 任务2：取指 (IF) 平均周期数与缓存命中/未命中统计
    // ---------------------------------------------------------
    if (io_if_begin) {
        is_fetching = true;
        current_if_counter = 0;
        current_if_missed = false; // 取指开始时，重置标志位
    }

    // --- 新增：如果在取指期间检测到了 miss 信号（一周期高电平），则锁存状态 ---
    if (is_fetching && io_if_miss) {
        current_if_missed = true;
    }

    if (is_fetching) {
        current_if_counter++;
    }

    if (io_if_finish) {
        if (INCLUDE_FLASH_DATA || current_if_counter <= FLASH_THRESHOLD) {
            total_if_cycles += current_if_counter;
            total_if_counts++;
            
            // --- 新增：结算本次取指是命中还是未命中 ---
            if (current_if_missed) {
                total_if_miss_cycles += current_if_counter;
                total_if_miss_counts++;
            } else {
                total_if_hit_cycles += current_if_counter;
                total_if_hit_counts++;
            }
        }
        is_fetching = false;
    }

    // ---------------------------------------------------------
    // 任务3：LSU 读/写 平均周期数
    // ---------------------------------------------------------
    if (io_lsu_r_begin) {
        is_lsur = true;
        current_lsur_counter = 0;
    }
    if (is_lsur) {
        current_lsur_counter++;
    }
    if (io_lsu_r_finish) {
        if (INCLUDE_FLASH_DATA || current_lsur_counter <= FLASH_THRESHOLD) {
            total_lsur_cycles += current_lsur_counter;
            total_lsur_counts++;
        }
        is_lsur = false;
    }

    if (io_lsu_w_begin) {
        is_lsuw = true;
        current_lsuw_counter = 0;
    }
    if (is_lsuw) {
        current_lsuw_counter++;
    }
    if (io_lsu_w_finish) {
        if (INCLUDE_FLASH_DATA || current_lsuw_counter <= FLASH_THRESHOLD) {
            total_lsuw_cycles += current_lsuw_counter;
            total_lsuw_counts++;
        }
        is_lsuw = false;
    }
}

// ==========================================
// 仿真结束时的打印与计算
// ==========================================
void print_performance_counters() {
    printf("\n=================================================================================================================\n");
    printf("                                         Performance Latency Analysis\n");
    printf("                                         [Mode: %s Flash Data]\n", 
            INCLUDE_FLASH_DATA ? "INCLUDING" : "EXCLUDING (>%d cycles)", FLASH_THRESHOLD);
    printf("=================================================================================================================\n\n");

    // 1. 计算总线的平均延迟
    double avg_if = total_if_counts ? (double)total_if_cycles / total_if_counts : 0.0;
    
    // --- 新增：计算命中率和分情况耗时 ---
    double if_hit_rate = total_if_counts ? ((double)total_if_hit_counts / total_if_counts * 100.0) : 0.0;
    double avg_if_hit  = total_if_hit_counts ? (double)total_if_hit_cycles / total_if_hit_counts : 0.0;
    double avg_if_miss = total_if_miss_counts ? (double)total_if_miss_cycles / total_if_miss_counts : 0.0;

    double avg_lsur = total_lsur_counts ? (double)total_lsur_cycles / total_lsur_counts : 0.0;
    double avg_lsuw = total_lsuw_counts ? (double)total_lsuw_cycles / total_lsuw_counts : 0.0;

    printf("--- Bus Transaction Latency ---\n");
    printf("Instruction Fetch (IF) : %6.2f cycles/req (Total Count: %lu)\n", avg_if, total_if_counts);
    // --- 打印取指缓存分析信息 ---
    printf("  -> IF Cache Hit Rate : %6.2f%% (Hit: %lu, Miss: %lu)\n", if_hit_rate, total_if_hit_counts, total_if_miss_counts);
    printf("  -> Avg Hit Latency   : %6.2f cycles/req\n", avg_if_hit);
    printf("  -> Avg Miss Latency  : %6.2f cycles/req\n", avg_if_miss);
    
    printf("LSU Read Latency       : %6.2f cycles/req (Total Count: %lu)\n", avg_lsur, total_lsur_counts);
    printf("LSU Write Latency      : %6.2f cycles/req (Total Count: %lu)\n", avg_lsuw, total_lsuw_counts);
    printf("\n");

    // =======================================================================
    // 2 & 3: 左右排版 - 指令周期与整体占比
    // =======================================================================

    // 提前计算整体周期和 IPC 数据
    uint64_t total_cycles = 0;
    uint64_t total_insts = 0;

    if (INCLUDE_FLASH_DATA || current_inst_cycle_counter <= FLASH_THRESHOLD) {
        total_cycles = current_inst_cycle_counter; 
    }
    for (int i = 0; i < TYPE_COUNT; i++) {
        total_cycles += inst_cycles[i];
        total_insts += inst_counts[i]; // 累加所有有效指令
    }

    uint64_t total_wait_cycles = total_if_cycles + total_lsur_cycles + total_lsuw_cycles;
    uint64_t active_cycles = (total_cycles >= total_wait_cycles) ? (total_cycles - total_wait_cycles) : 0;
    
    double ipc = total_cycles > 0 ? ((double)total_insts / total_cycles) : 0.0;
    double pct_active = total_cycles ? ((double)active_cycles / total_cycles * 100.0) : 0.0;
    double pct_if     = total_cycles ? ((double)total_if_cycles / total_cycles * 100.0) : 0.0;
    double pct_lsur   = total_cycles ? ((double)total_lsur_cycles / total_cycles * 100.0) : 0.0;
    double pct_lsuw   = total_cycles ? ((double)total_lsuw_cycles / total_cycles * 100.0) : 0.0;

    // 行缓存矩阵，用于左右对比打印
    char left_col[15][128] = {0};
    char right_col[15][128] = {0};
    int left_lines = 0;
    int right_lines = 0;

    // --- 填充左列 (Cycles to Next Fetch) ---
    snprintf(left_col[left_lines++], 128, "--- Cycles to Next Fetch by Inst ---");
    for (int i = 1; i < TYPE_COUNT; i++) {
        if (inst_counts[i] > 0) {
            double avg_cycles = (double)inst_cycles[i] / inst_counts[i];
            snprintf(left_col[left_lines++], 128, "%-10s: %6.2f cycles (Count: %6lu)", 
                     inst_names[i], avg_cycles, inst_counts[i]);
        }
    }

    // --- 填充右列 (Overall Cycle Breakdown) ---
    if (total_cycles > 0) {
        snprintf(right_col[right_lines++], 128, "--- Overall Cycle Breakdown (Total = 100%%) ---");
        snprintf(right_col[right_lines++], 128, "%-20s | %-30s", "Category", "Cycles & Percentage");
        snprintf(right_col[right_lines++], 128, "---------------------|-----------------------------------");
        snprintf(right_col[right_lines++], 128, "%-20s | %lu cycles", "Total Target Cycles", total_cycles);
        snprintf(right_col[right_lines++], 128, "%-20s | %6.2f%% (%lu cycles)", "Active Cycles (Work)", pct_active, active_cycles);
        snprintf(right_col[right_lines++], 128, "%-20s | %6.2f%% (%lu cycles)", "Wait for IF   (Read)", pct_if, total_if_cycles);
        snprintf(right_col[right_lines++], 128, "%-20s | %6.2f%% (%lu cycles)", "Wait for LSU  (Read)", pct_lsur, total_lsur_cycles);
        snprintf(right_col[right_lines++], 128, "%-20s | %6.2f%% (%lu cycles)", "Wait for LSU (Write)", pct_lsuw, total_lsuw_cycles);
        snprintf(right_col[right_lines++], 128, "---------------------|-----------------------------------");
        snprintf(right_col[right_lines++], 128, "%-20s | %6.2f%%", "Sum of Percentages", pct_active + pct_if + pct_lsur + pct_lsuw);
        
        // 增加 IPC (5位小数) 打印
        snprintf(right_col[right_lines++], 128, "%-20s | %.5f", "IPC (Insts/Cycle)", ipc);
    }

    // --- 并排打印 ---
    int max_lines = left_lines > right_lines ? left_lines : right_lines;
    for (int i = 0; i < max_lines; i++) {
        // 左列占据 45 个字符的宽度，不够的用空格补齐，中间用 || 隔开
        printf("%-45s ||  %s\n", left_col[i], right_col[i]);
    }
    printf("\n");

    if (total_wait_cycles > total_cycles) {
        printf("* Note: Wait cycles may exceed total cycles due to bus overlaps or data alignment.\n");
    }

    printf("=================================================================================================================\n\n");
}