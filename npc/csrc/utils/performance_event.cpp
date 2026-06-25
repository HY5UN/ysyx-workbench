#include <svdpi.h>
#include <stdint.h>
#include <stdio.h>
#include "include/common.h"
#include "include/CPU.h"

// ==========================================
// 统计开关配置
// ==========================================
// 开关：决定是否统计 Flash 数据（耗时 > 100 周期的操作）
// - true : 统计全部数据（包含 Flash 长周期）
// - false: 仅统计周期数 <= 100 的短周期数据
const bool INCLUDE_FLASH_DATA = true; 
const uint64_t FLASH_THRESHOLD = 100;

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

// 3. LSU 读周期统计
uint64_t total_lsur_cycles = 0;
uint64_t total_lsur_counts = 0;
uint64_t current_lsur_counter = 0;
bool is_lsur = false;

// 4. LSU 写周期统计 (通常无Flash，但统一风格)
uint64_t total_lsuw_cycles = 0;
uint64_t total_lsuw_counts = 0;
uint64_t current_lsuw_counter = 0;
bool is_lsuw = false;


// ==========================================
// DPI-C 周期采样函数 (每周期执行)
// ==========================================
extern "C" void dpic_save_performance_event(
    svBit io_if_begin,
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
            // 根据开关与阈值判断是否计入统计
            if (INCLUDE_FLASH_DATA || current_inst_cycle_counter <= FLASH_THRESHOLD) {
                inst_cycles[current_inst] += current_inst_cycle_counter;
                inst_counts[current_inst]++;
            }
        }
        // 重置状态
        current_inst = NONE; 
        current_inst_cycle_counter = 0; 
    } 
    
    current_inst_cycle_counter++;

    // 捕获新的指令类型
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
    // 任务2：取指 (IF) 平均周期数
    // ---------------------------------------------------------
    if (io_if_begin) {
        is_fetching = true;
        current_if_counter = 0;
    }
    if (is_fetching) {
        current_if_counter++;
    }
    if (io_if_finish) {
        if (INCLUDE_FLASH_DATA || current_if_counter <= FLASH_THRESHOLD) {
            total_if_cycles += current_if_counter;
            total_if_counts++;
        }
        is_fetching = false;
    }

    // ---------------------------------------------------------
    // 任务3：LSU 读/写 平均周期数
    // ---------------------------------------------------------
    // LSU 读
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

    // LSU 写 
    if (io_lsu_w_begin) {
        is_lsuw = true;
        current_lsuw_counter = 0;
    }
    if (is_lsuw) {
        current_lsuw_counter++;
    }
    if (io_lsu_w_finish) {
        // 通常写无Flash，直接复用开关逻辑保持统一
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
    // 动态展示当前的统计模式
    printf("                                         [Mode: %s Flash Data]\n", 
            INCLUDE_FLASH_DATA ? "INCLUDING" : "EXCLUDING (>100 cycles)");
    printf("=================================================================================================================\n\n");

    // 1. 计算总线的平均延迟（横向排版）
    double avg_if   = total_if_counts   ? (double)total_if_cycles   / total_if_counts   : 0.0;
    double avg_lsur = total_lsur_counts ? (double)total_lsur_cycles / total_lsur_counts : 0.0;
    double avg_lsuw = total_lsuw_counts ? (double)total_lsuw_cycles / total_lsuw_counts : 0.0;

    printf("[1. Bus Transaction Latency]\n");
    printf("%-15s | %-20s | %-15s\n", "Transaction", "Avg Latency (cycles)", "Total Requests");
    printf("----------------|----------------------|----------------\n");
    printf("%-15s | %-20.2f | %-15lu\n", "Inst Fetch", avg_if, total_if_counts);
    printf("%-15s | %-20.2f | %-15lu\n", "LSU Read", avg_lsur, total_lsur_counts);
    printf("%-15s | %-20.2f | %-15lu\n\n", "LSU Write", avg_lsuw, total_lsuw_counts);

    // 2. 计算并打印各类指令的平均等待周期（4列紧凑排版）
    printf("[2. Cycles to Next Fetch by Instruction Type]\n");
    int col = 0;
    for (int i = 1; i < TYPE_COUNT; i++) {
        if (inst_counts[i] > 0) {
            double avg_cycles = (double)inst_cycles[i] / inst_counts[i];
            // 采用 28 个字符长度对齐，一排容纳 4 个指令类型
            printf("%-8s: %5.2f (C:%6lu) | ", inst_names[i], avg_cycles, inst_counts[i]);
            col++;
            if (col % 4 == 0) printf("\n");
        }
    }
    if (col % 4 != 0) printf("\n");
    printf("\n");

    // ---------------------------------------------------------
    // 3. 计算整体 IPC 和周期占比
    // ---------------------------------------------------------
    // 统计总周期与总指令数
    uint64_t total_cycles = 0;
    uint64_t total_insts = 0;
    
    if (INCLUDE_FLASH_DATA || current_inst_cycle_counter <= FLASH_THRESHOLD) {
        total_cycles = current_inst_cycle_counter; // 累加最后一条还未结束的指令
    }
    
    for (int i = 0; i < TYPE_COUNT; i++) {
        total_cycles += inst_cycles[i];
        total_insts  += inst_counts[i];
    }

    uint64_t total_wait_cycles = total_if_cycles + total_lsur_cycles + total_lsuw_cycles;
    
    // 有事发生(CPU真正在干活的)周期 = 总周期 - 等待周期
    uint64_t active_cycles = 0;
    if (total_cycles >= total_wait_cycles) {
        active_cycles = total_cycles - total_wait_cycles;
    }

    double ipc = total_cycles ? (double)total_insts / total_cycles : 0.0;

    if (total_cycles > 0) {
        double pct_active = (double)active_cycles / total_cycles * 100.0;
        double pct_if     = (double)total_if_cycles / total_cycles * 100.0;
        double pct_lsur   = (double)total_lsur_cycles / total_cycles * 100.0;
        double pct_lsuw   = (double)total_lsuw_cycles / total_cycles * 100.0;

        printf("[3. Overall Performance & Cycle Breakdown]\n");
        printf("Total Cycles : %-16lu | Total Insts : %-16lu | IPC : %.5f\n", total_cycles, total_insts, ipc);
        printf("-----------------------------------------------------------------------------------------------------------------\n");
        
        // 横向铺开各状态周期占比
        printf("Active  : %-8lu (%5.2f%%) | Wait IF : %-8lu (%5.2f%%) | Wait LSU-R: %-8lu (%5.2f%%) | Wait LSU-W: %-8lu (%5.2f%%)\n", 
                active_cycles, pct_active, 
                total_if_cycles, pct_if, 
                total_lsur_cycles, pct_lsur, 
                total_lsuw_cycles, pct_lsuw);
        
        if (total_wait_cycles > total_cycles) {
            printf("\n* Note: Wait cycles may exceed total cycles due to bus overlaps or data alignment.\n");
        }
    }

    printf("=================================================================================================================\n\n");
}