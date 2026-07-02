#include <svdpi.h>
#include <stdint.h>
#include <stdio.h>
#include <string.h>
#include "include/common.h"
#include "include/CPU.h"
#include <svdpi.h>
#include <iostream>
#include <iomanip>
#include <stdint.h>
#include <queue>
#include <string>

// ==========================================
// 全局状态与统计计数器
// ==========================================
static bool     pfm_started = false;
static uint64_t total_cycles = 0;

// 取指阶段 (IF)
static bool     if_active = false;
static uint64_t if_start_cycle = 0;
static bool     if_current_missed = false;

static uint64_t if_total_reqs = 0;
static uint64_t if_hit_reqs = 0;
static uint64_t if_miss_reqs = 0;
static uint64_t if_total_cycles = 0;
static uint64_t if_hit_cycles = 0;
static uint64_t if_miss_cycles = 0;

// 取指总线访问 (IF Bus)
static bool     if_bus_active = false;
static uint64_t if_bus_start_cycle = 0;
static uint64_t if_bus_reqs = 0;
static uint64_t if_bus_total_cycles = 0;

// 访存阶段 (LSU)
static bool     lsu_r_active = false;
static uint64_t lsu_r_start_cycle = 0;
static uint64_t lsu_r_reqs = 0;
static uint64_t lsu_r_total_cycles = 0;

static bool     lsu_w_active = false;
static uint64_t lsu_w_start_cycle = 0;
static uint64_t lsu_w_reqs = 0;
static uint64_t lsu_w_total_cycles = 0;

// 流水线停顿 (Stall)
static uint64_t ifu_stall_cycles = 0;
static uint64_t lsu_stall_cycles = 0;

// 提交与指令类型 (Commit & Instructions)
static uint64_t commit_count = 0;

enum InstType { R, I, L, S, U, B, J, CSR, SYS, NUM_TYPES };
const std::string inst_names[NUM_TYPES] = {"R-Type", "I-Type", "L-Type", "S-Type", "U-Type", "B-Type", "J-Type", "CSR", "SYS"};
static uint64_t inst_counts[NUM_TYPES] = {0};
static uint64_t inst_exec_cycles[NUM_TYPES] = {0};

// 用于匹配 if_finish 和 wbu_valid 的时间戳队列
static std::queue<uint64_t> if_finish_queue;


// ==========================================
// DPI-C 周期采样函数 (每周期执行)
// ==========================================
extern "C" void dpic_save_performance_event(
    svBit io_pfm_begin,
    svBit io_if_begin,
    svBit io_if_miss,
    svBit io_if_finish,
    svBit io_ifu_nvalid,
    svBit io_if_bus_req,
    svBit io_if_bus_resp,
    svBit io_lsu_r_begin,
    svBit io_lsu_r_finish,
    svBit io_lsu_w_begin,   
    svBit io_lsu_w_finish,
    svBit io_lsu_nvalid,
    svBit io_wbu_valid,
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
    // 检查是否开启统计
    if (io_pfm_begin == 1) {
        pfm_started = true;
    }
    if (!pfm_started) return;

    total_cycles++;

    // --- 1. IF Fetch 追踪 ---
    if (io_if_begin) {
        if_active = true;
        if_start_cycle = total_cycles;
        if_current_missed = false; // 复位 miss 标志
    }
    
    // miss 信号在 begin 和 finish 之间拉高，捕捉它
    if (if_active && io_if_miss) {
        if_current_missed = true;
    }

    if (if_active && io_if_finish) {
        uint64_t cycles_spent = total_cycles - if_start_cycle;
        if_total_reqs++;
        if_total_cycles += cycles_spent;

        if (if_current_missed) {
            if_miss_reqs++;
            if_miss_cycles += cycles_spent;
        } else {
            if_hit_reqs++;
            if_hit_cycles += cycles_spent;
        }

        if_active = false;
        
        // 将完成周期压入队列，供后端计算执行时间
        if_finish_queue.push(total_cycles);
    }

    // --- 2. 取指总线追踪 ---
    if (io_if_bus_req) {
        if_bus_active = true;
        if_bus_start_cycle = total_cycles;
    }
    if (if_bus_active && io_if_bus_resp) {
        if_bus_reqs++;
        if_bus_total_cycles += (total_cycles - if_bus_start_cycle);
        if_bus_active = false;
    }

    // --- 3. LSU 读写追踪 ---
    // Read
    if (io_lsu_r_begin) {
        lsu_r_active = true;
        lsu_r_start_cycle = total_cycles;
    }
    if (lsu_r_active && io_lsu_r_finish) {
        lsu_r_reqs++;
        lsu_r_total_cycles += (total_cycles - lsu_r_start_cycle);
        lsu_r_active = false;
    }

    // Write
    if (io_lsu_w_begin) {
        lsu_w_active = true;
        lsu_w_start_cycle = total_cycles;
    }
    if (lsu_w_active && io_lsu_w_finish) {
        lsu_w_reqs++;
        lsu_w_total_cycles += (total_cycles - lsu_w_start_cycle);
        lsu_w_active = false;
    }

    // --- 4. 停顿流水线采样 ---
    if (io_ifu_nvalid) ifu_stall_cycles++;
    if (io_lsu_nvalid) lsu_stall_cycles++;

    // --- 5. 提交(WBU)与指令类型采样 ---
    if (io_wbu_valid) {
        commit_count++;
        
        uint64_t exec_cycles = 0;
        // 如果没有 flush/异常分支预测失败，队列应当完全对应
        if (!if_finish_queue.empty()) {
            exec_cycles = total_cycles - if_finish_queue.front();
            if_finish_queue.pop();
        }

        // 识别指令类型
        InstType type = NUM_TYPES;
        if (io_inst_r) type = R;
        else if (io_inst_i) type = I;
        else if (io_inst_l) type = L;
        else if (io_inst_s) type = S;
        else if (io_inst_u) type = U;
        else if (io_inst_b) type = B;
        else if (io_inst_j) type = J;
        else if (io_inst_csr) type = CSR;
        else if (io_inst_sys) type = SYS;

        if (type != NUM_TYPES) {
            inst_counts[type]++;
            inst_exec_cycles[type] += exec_cycles;
        }
    }
}

// ==========================================
// 性能计数器打印函数
// ==========================================
extern "C" void print_performance_counters() {
    if (!pfm_started || total_cycles == 0) {
        std::cout << "========== Performance Counters ==========\n";
        std::cout << "Error: Performance monitoring did not start or ran for 0 cycles.\n";
        std::cout << "========================================\n";
        return;
    }

    // 安全的除法宏
    #define SAFE_DIV(a, b) ((b) == 0 ? 0.0 : (double)(a) / (b))
    #define PCT(a, b)      (SAFE_DIV(a, b) * 100.0)

    std::cout << "\n=======================================================\n";
    std::cout << "               CPU PERFORMANCE REPORT                  \n";
    std::cout << "=======================================================\n";
    std::cout << "Total Active Cycles      : " << total_cycles << "\n";
    std::cout << "Total Commits (IPC)      : " << commit_count 
              << " (" << std::fixed << std::setprecision(4) << SAFE_DIV(commit_count, total_cycles) << ")\n";
    std::cout << "Commit Active Cycle %    : " << std::fixed << std::setprecision(2) << PCT(commit_count, total_cycles) << "%\n";
    std::cout << "-------------------------------------------------------\n";
    
    // --- 取指统计 ---
    double hit_rate = PCT(if_hit_reqs, if_total_reqs);
    std::cout << "[Instruction Fetch (IF)]\n";
    std::cout << "Total Fetch Requests     : " << if_total_reqs << "\n";
    std::cout << "Fetch Hit Rate           : " << std::fixed << std::setprecision(2) << hit_rate << "%\n";
    std::cout << "Avg Cycles per Fetch     : " << std::fixed << std::setprecision(2) << SAFE_DIV(if_total_cycles, if_total_reqs) << " cycles\n";
    std::cout << "Avg Cycles (Hit)         : " << std::fixed << std::setprecision(2) << SAFE_DIV(if_hit_cycles, if_hit_reqs) << " cycles\n";
    std::cout << "Avg Cycles (Miss)        : " << std::fixed << std::setprecision(2) << SAFE_DIV(if_miss_cycles, if_miss_reqs) << " cycles\n";
    std::cout << "Avg Fetch Bus Latency    : " << std::fixed << std::setprecision(2) << SAFE_DIV(if_bus_total_cycles, if_bus_reqs) << " cycles\n";
    std::cout << "-------------------------------------------------------\n";

    // --- LSU 统计 ---
    std::cout << "[Load/Store Unit (LSU)]\n";
    std::cout << "Avg Cycles per LSU Read  : " << std::fixed << std::setprecision(2) << SAFE_DIV(lsu_r_total_cycles, lsu_r_reqs) << " cycles\n";
    std::cout << "Avg Cycles per LSU Write : " << std::fixed << std::setprecision(2) << SAFE_DIV(lsu_w_total_cycles, lsu_w_reqs) << " cycles\n";
    std::cout << "-------------------------------------------------------\n";

    // --- 流水线卡顿比例 ---
    std::cout << "[Pipeline Stalls]\n";
    std::cout << "IFU Stall Ratio          : " << std::fixed << std::setprecision(2) << PCT(ifu_stall_cycles, total_cycles) << "%\n";
    std::cout << "LSU Stall Ratio          : " << std::fixed << std::setprecision(2) << PCT(lsu_stall_cycles, total_cycles) << "%\n";
    std::cout << "-------------------------------------------------------\n";

    // --- 指令级统计 ---
    std::cout << "[Instruction Distribution & Execution Latency]\n";
    std::cout << std::left << std::setw(10) << "Type" 
              << std::setw(15) << "Count" 
              << std::setw(15) << "Ratio (%)" 
              << std::setw(15) << "Avg Exec Cycles (if_finish -> WBU)" << "\n";
    
    for (int i = 0; i < NUM_TYPES; ++i) {
        if (inst_counts[i] > 0) {
            double ratio = PCT(inst_counts[i], commit_count);
            double avg_cycles = SAFE_DIV(inst_exec_cycles[i], inst_counts[i]);
            
            std::cout << std::left << std::setw(10) << inst_names[i] 
                      << std::setw(15) << inst_counts[i] 
                      << std::fixed << std::setprecision(2) << std::setw(15) << ratio 
                      << std::fixed << std::setprecision(2) << std::setw(15) << avg_cycles << "\n";
        }
    }
    std::cout << "=======================================================\n";
    
    // 检查潜在的 flush 问题（通常在分支预测失败时，队列可能会堆积残余时间戳）
    if (!if_finish_queue.empty()) {
         std::cout << "* Note: if_finish_queue has " << if_finish_queue.size() 
                   << " items left (Likely pipeline flushed/squashed instructions).\n";
    }
}