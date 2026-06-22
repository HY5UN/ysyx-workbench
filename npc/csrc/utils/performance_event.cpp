#include <svdpi.h>
#include <stdint.h>
#include <stdio.h>
#include "include/common.h"
#include "include/CPU.h"
// ==========================================
// 全局状态与计数器定义
// ==========================================

// 1. 各类指令执行周期统计
// 定义指令类型枚举，方便数组索引
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
    // 发生下一次取指时，结算上一次记录的指令周期
    if (io_if_begin) {
        if (current_inst != NONE) {
            inst_cycles[current_inst] += current_inst_cycle_counter;
            inst_counts[current_inst]++;
            current_inst = NONE;             // 结算完毕，清空当前指令状态
            current_inst_cycle_counter = 0; 
        }
    } 
    
    // 如果没有遇到新的取指，且当前有被捕获的指令，则持续增加周期
    if (current_inst != NONE) {
        current_inst_cycle_counter++;
    }

    // 捕获新的指令类型 (通常在译码阶段或EX阶段拉高)
    // 优先级判断，假设这些信号同一周期只有一个为1
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
        if(current_lsur_counter > 40){
            printf("[Warning] Detected a long LSU read latency: %lu cycles, cycle: %lu\n", current_lsur_counter, cpu->cycle_count);
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
        total_lsuw_cycles += current_lsuw_counter;
        total_lsuw_counts++;
        is_lsuw = false;
    }
}

// ==========================================
// 仿真结束时的打印与计算
// ==========================================
void print_performance_counters() {
    printf("\n=========================================\n");
    printf("     Performance Latency Analysis\n");
    printf("=========================================\n\n");

    // 计算并打印取指和访存的平均延迟
    double avg_if = total_if_counts ? (double)total_if_cycles / total_if_counts : 0.0;
    double avg_lsur = total_lsur_counts ? (double)total_lsur_cycles / total_lsur_counts : 0.0;
    double avg_lsuw = total_lsuw_counts ? (double)total_lsuw_cycles / total_lsuw_counts : 0.0;

    printf("--- Bus Transaction Latency ---\n");
    printf("Instruction Fetch (IF) : %.2f cycles/req  (Total Count: %lu)\n", avg_if, total_if_counts);
    printf("LSU Read Latency       : %.2f cycles/req  (Total Count: %lu)\n", avg_lsur, total_lsur_counts);
    printf("LSU Write Latency      : %.2f cycles/req  (Total Count: %lu)\n", avg_lsuw, total_lsuw_counts);
    printf("\n");

    // 计算并打印各类指令的平均等待周期
    printf("--- Cycles to Next Fetch by Instruction Type ---\n");
    for (int i = 1; i < TYPE_COUNT; i++) {
        if (inst_counts[i] > 0) {
            double avg_cycles = (double)inst_cycles[i] / inst_counts[i];
            // 使用 %-10s 保证左对齐，使得输出更美观
            printf("%-10s: %.2f cycles (Count: %lu)\n", inst_names[i], avg_cycles, inst_counts[i]);
        }
    }
    printf("=========================================\n\n");
}