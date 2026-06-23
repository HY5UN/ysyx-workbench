#include <svdpi.h>
#include <stdint.h>
#include <stdio.h>
#include "include/common.h"
#include "include/CPU.h"

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

// 包含 Flash 的指令统计
uint64_t inst_cycles[TYPE_COUNT] = {0}; // 记录某种指令到下一次取指花费的总周期
uint64_t inst_counts[TYPE_COUNT] = {0}; // 记录某种指令的总条数

// 【新增】不包含 Flash 的指令统计（周期数 <= 100）
uint64_t inst_cycles_no_flash[TYPE_COUNT] = {0}; 
uint64_t inst_counts_no_flash[TYPE_COUNT] = {0}; 

InstType current_inst = NONE;           // 当前正在执行的指令类型
uint64_t current_inst_cycle_counter = 0;// 当前指令正在累加的周期

// 2. 取指 (IF) 周期统计
uint64_t total_if_cycles = 0;
uint64_t total_if_counts = 0;
uint64_t total_if_cycles_no_flash = 0;  // 排除Flash的周期统计
uint64_t total_if_counts_no_flash = 0;  // 排除Flash的次数统计
uint64_t current_if_counter = 0;
bool is_fetching = false;

// 3. LSU 读周期统计
uint64_t total_lsur_cycles = 0;
uint64_t total_lsur_counts = 0;
uint64_t total_lsur_cycles_no_flash = 0; // 排除Flash的周期统计
uint64_t total_lsur_counts_no_flash = 0; // 排除Flash的次数统计
uint64_t current_lsur_counter = 0;
bool is_lsur = false;

// 4. LSU 写周期统计 (无需排除Flash)
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
    
    // 发生下一次取指时，结算上一条指令的周期
    if (io_if_begin) {
        // 如果在此之前捕获到了有效的指令类型，则把这段时间的周期数累加到该类型上
        if (current_inst != NONE) {
            // 1. 无论周期多大，正常累加到包含 Flash 的全局统计中
            inst_cycles[current_inst] += current_inst_cycle_counter;
            inst_counts[current_inst]++;

            // 2. 【修改点】：如果当前指令花费的总周期 <= 100，说明未触发 Flash 读，计入 no_flash 统计
            if (current_inst_cycle_counter <= 100) {
                inst_cycles_no_flash[current_inst] += current_inst_cycle_counter;
                inst_counts_no_flash[current_inst]++;
            }
        }
        
        // 无论上一条是否有效，遇到新的取指就要重置状态，并准备重新计数
        current_inst = NONE; 
        current_inst_cycle_counter = 0; 
    } 
    
    // 从取指开始（即使还不知道指令类型），无条件持续增加周期
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
        total_if_cycles += current_if_counter;
        total_if_counts++;
        
        // IF 周期数 <= 100 时，才计入 no_flash 统计
        if (current_if_counter <= 100) {
            total_if_cycles_no_flash += current_if_counter;
            total_if_counts_no_flash++;
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
        total_lsur_cycles += current_lsur_counter;
        total_lsur_counts++;
        
        // LSU读 周期数 <= 100 时，才计入 no_flash 统计
        if (current_lsur_counter <= 100) {
            total_lsur_cycles_no_flash += current_lsur_counter;
            total_lsur_counts_no_flash++;
        }
        
        is_lsur = false;
    }

    // LSU 写 (无需过滤 Flash)
    if (io_lsu_w_begin) {
        is_lsuw = true;
        current_lsuw_counter = 0;
    }
    if (is_lsuw) {
        current_lsuw_counter++;
    }
    if (io_lsuw_finish) {
        total_lsuw_cycles += current_lsuw_counter;
        total_lsuw_counts++;
        is_lsuw = false;
    }
}

// ==========================================
// 仿真结束时的打印与计算
// ==========================================
void print_performance_counters() {
    printf("\n=================================================================================================================\n");
    printf("                                          Performance Latency Analysis\n");
    printf("=================================================================================================================\n\n");

    // 1. 计算总线的平均延迟
    double avg_if = total_if_counts ? (double)total_if_cycles / total_if_counts : 0.0;
    double avg_lsur = total_lsur_counts ? (double)total_lsur_cycles / total_lsur_counts : 0.0;
    double avg_lsuw = total_lsuw_counts ? (double)total_lsuw_cycles / total_lsuw_counts : 0.0;

    double avg_if_nf = total_if_counts_no_flash ? (double)total_if_cycles_no_flash / total_if_counts_no_flash : 0.0;
    double avg_lsur_nf = total_lsur_counts_no_flash ? (double)total_lsur_cycles_no_flash / total_lsur_counts_no_flash : 0.0;

    printf("--- Bus Transaction Latency ---\n");
    printf("Instruction Fetch (IF) : %6.2f cycles/req | No-Flash(<=100): %6.2f cycles/req  (Total Count: %lu, NF Count: %lu)\n", 
            avg_if, avg_if_nf, total_if_counts, total_if_counts_no_flash);
    printf("LSU Read Latency       : %6.2f cycles/req | No-Flash(<=100): %6.2f cycles/req  (Total Count: %lu, NF Count: %lu)\n", 
            avg_lsur, avg_lsur_nf, total_lsur_counts, total_lsur_counts_no_flash);
    printf("LSU Write Latency      : %6.2f cycles/req | (All writes are Non-Flash, Total Count: %lu)\n", 
            avg_lsuw, total_lsuw_counts);
    printf("\n");

    // 2. 计算并打印各类指令的平均等待周期
    printf("--- Cycles to Next Fetch by Instruction Type ---\n");
    for (int i = 1; i < TYPE_COUNT; i++) {
        // 只要在任何一个统计中有数据，就打印该指令行
        if (inst_counts[i] > 0 || inst_counts_no_flash[i] > 0) {
            // 计算原结果（包含 Flash）
            double avg_cycles = inst_counts[i] ? (double)inst_cycles[i] / inst_counts[i] : 0.0;
            
            // 计算排除 Flash 后的结果
            double avg_cycles_nf = inst_counts_no_flash[i] ? (double)inst_cycles_no_flash[i] / inst_counts_no_flash[i] : 0.0;

            // 【修改点】：将不包含 Flash 的指令统计打印在原结果的右边
            printf("%-10s: %6.2f cycles (Count: %6lu) | No-Flash(<=100): %6.2f cycles (Count: %6lu)\n", 
                    inst_names[i], 
                    avg_cycles, inst_counts[i], 
                    avg_cycles_nf, inst_counts_no_flash[i]);
        }
    }
    printf("=================================================================================================================\n\n");
}