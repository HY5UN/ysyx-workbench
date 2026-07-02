#include <svdpi.h>
#include <iostream>
#include <iomanip>
#include <stdint.h>
#include <string>

// ==========================================
// 全局状态与统计计数器
// ==========================================
static bool pfm_started = false;
static uint64_t total_cycles = 0;

// --- 定义静态变量保存上一个周期的状态用于边沿检测 ---
static svBit prev_io_if_begin = 0;
static svBit prev_io_if_bus_req = 0;
static svBit prev_io_lsu_r_begin = 0;
static svBit prev_io_lsu_w_begin = 0;

// 取指阶段 (IF)
static bool if_active = false;
static uint64_t if_start_cycle = 0;
static bool if_current_missed = false;

static uint64_t if_total_reqs = 0;
static uint64_t if_hit_reqs = 0;
static uint64_t if_miss_reqs = 0;
static uint64_t if_total_cycles = 0;
static uint64_t if_hit_cycles = 0;
static uint64_t if_miss_cycles = 0;

// 取指总线访问 (IF Bus)
static bool if_bus_active = false;
static uint64_t if_bus_start_cycle = 0;
static uint64_t if_bus_reqs = 0;
static uint64_t if_bus_total_cycles = 0;

// 访存阶段 (LSU)
static bool lsu_r_active = false;
static uint64_t lsu_r_start_cycle = 0;
static uint64_t lsu_r_reqs = 0;
static uint64_t lsu_r_total_cycles = 0;

static bool lsu_w_active = false;
static uint64_t lsu_w_start_cycle = 0;
static uint64_t lsu_w_reqs = 0;
static uint64_t lsu_w_total_cycles = 0;

// 流水线停顿 (Stall)
static uint64_t ifu_stall_cycles = 0;
static uint64_t lsu_stall_cycles = 0;

// 提交与指令类型 (Commit & Instructions)
static uint64_t commit_count = 0;

enum InstType
{
    R,
    I,
    L,
    S,
    U,
    B,
    J,
    CSR,
    SYS,
    NUM_TYPES
};
const std::string inst_names[NUM_TYPES] = {"R-Type", "I-Type", "L-Type", "S-Type", "U-Type", "B-Type", "J-Type", "CSR", "SYS"};
static uint64_t inst_counts[NUM_TYPES] = {0};
static uint64_t inst_exec_cycles[NUM_TYPES] = {0};

// ==========================================
// 惰性覆盖 (Lazy Overwrite) Tag 时间戳记录表
// 假设 char 为 8位，最大 256 种 Tag 状态，足以覆盖任何深度的经典流水线
// ==========================================
static uint64_t inst_start_time[256] = {0};

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
    char io_if_tag,

    svBit io_lsu_r_begin,
    svBit io_lsu_r_finish,
    svBit io_lsu_w_begin,
    svBit io_lsu_w_finish,
    svBit io_lsu_nvalid,

    svBit io_wbu_valid,
    char io_wbu_tag,

    svBit io_inst_r,
    svBit io_inst_i,
    svBit io_inst_l,
    svBit io_inst_s,
    svBit io_inst_u,
    svBit io_inst_b,
    svBit io_inst_j,
    svBit io_inst_csr,
    svBit io_inst_sys)
{

    // 检查是否开启统计
    if (io_pfm_begin == 1)
    {
        pfm_started = true;
    }
    if (!pfm_started)
    {
        return;
    }

    total_cycles++;

    // --- 边沿检测逻辑 (当前为1，且上一周期为0) ---
    bool is_if_begin_posedge = (io_if_begin == 1 && prev_io_if_begin == 0);
    bool is_if_bus_req_posedge = (io_if_bus_req == 1 && prev_io_if_bus_req == 0);
    bool is_lsu_r_begin_posedge = (io_lsu_r_begin == 1 && prev_io_lsu_r_begin == 0);
    bool is_lsu_w_begin_posedge = (io_lsu_w_begin == 1 && prev_io_lsu_w_begin == 0);

    // --- 1. IF Fetch 追踪 ---
    if (is_if_begin_posedge)
    { // 替换为上升沿判断
        if_active = true;
        if_start_cycle = total_cycles;
        if_current_missed = false; // 复位 miss 标志
    }

    // miss 信号在 begin 和 finish 之间拉高，捕捉它
    if (if_active && io_if_miss)
    {
        if_current_missed = true;
    }

    if (if_active && io_if_finish)
    {
        uint64_t cycles_spent = total_cycles - if_start_cycle;
        if_total_reqs++;
        if_total_cycles += cycles_spent;

        if (if_current_missed)
        {
            if_miss_reqs++;
            if_miss_cycles += cycles_spent;
        }
        else
        {
            if_hit_reqs++;
            if_hit_cycles += cycles_spent;
        }

        if_active = false;

        // 【核心变更】将当前周期写入 Tag 对应的槽位。若该 Tag 之前有未提交的“幽灵指令”，将被静默覆盖。
        uint8_t safe_tag = (uint8_t)io_if_tag;
        inst_start_time[safe_tag] = total_cycles;
    }

    // --- 2. 取指总线追踪 ---
    if (is_if_bus_req_posedge)
    { // 替换为上升沿判断
        if_bus_active = true;
        if_bus_start_cycle = total_cycles;
    }
    if (if_bus_active && io_if_bus_resp)
    {
        if_bus_reqs++;
        if_bus_total_cycles += (total_cycles - if_bus_start_cycle);
        if_bus_active = false;
    }

    // --- 3. LSU 读写追踪 ---
    // Read
    if (is_lsu_r_begin_posedge)
    { // 替换为上升沿判断
        lsu_r_active = true;
        lsu_r_start_cycle = total_cycles;
    }
    if (lsu_r_active && io_lsu_r_finish)
    {
        lsu_r_reqs++;
        lsu_r_total_cycles += (total_cycles - lsu_r_start_cycle);
        lsu_r_active = false;
    }

    // Write
    if (is_lsu_w_begin_posedge)
    { // 替换为上升沿判断
        lsu_w_active = true;
        lsu_w_start_cycle = total_cycles;
    }
    if (lsu_w_active && io_lsu_w_finish)
    {
        lsu_w_reqs++;
        lsu_w_total_cycles += (total_cycles - lsu_w_start_cycle);
        lsu_w_active = false;
    }

    // --- 4. 停顿流水线采样 ---
    if (io_ifu_nvalid)
        ifu_stall_cycles++;
    if (io_lsu_nvalid)
        lsu_stall_cycles++;

    // --- 5. 提交(WBU)与指令类型采样 ---
    if (io_wbu_valid)
    {
        commit_count++;

        // 【核心变更】通过 WBU 送来的 Tag 查表，精准计算该指令在后段流水线的执行周期
        uint8_t safe_wbu_tag = (uint8_t)io_wbu_tag;
        uint64_t start_cycle = inst_start_time[safe_wbu_tag];
        uint64_t exec_cycles = 0;

        // 防御性编程：确保起始周期有效（大于0且不大于当前周期）
        if (start_cycle > 0 && start_cycle <= total_cycles)
        {
            exec_cycles = total_cycles - start_cycle;
        }

        // 识别指令类型
        InstType type = NUM_TYPES;
        if (io_inst_r)
            type = R;
        else if (io_inst_i)
            type = I;
        else if (io_inst_l)
            type = L;
        else if (io_inst_s)
            type = S;
        else if (io_inst_u)
            type = U;
        else if (io_inst_b)
            type = B;
        else if (io_inst_j)
            type = J;
        else if (io_inst_csr)
            type = CSR;
        else if (io_inst_sys)
            type = SYS;

        if (type != NUM_TYPES)
        {
            inst_counts[type]++;
            inst_exec_cycles[type] += exec_cycles;
        }
    }

    // --- 记录当前周期状态，供下个周期进行边沿判断 ---
    prev_io_if_begin = io_if_begin;
    prev_io_if_bus_req = io_if_bus_req;
    prev_io_lsu_r_begin = io_lsu_r_begin;
    prev_io_lsu_w_begin = io_lsu_w_begin;
}

// ==========================================
// 性能计数器打印函数
// ==========================================
#include <iostream>
#include <iomanip>
#include <string>
#include <sstream>

void print_performance_counters()
{
    if (!pfm_started || total_cycles == 0)
    {
        std::cout << "========================== Performance Counters ==========================\n";
        std::cout << "Error: Performance monitoring did not start or ran for 0 cycles.\n";
        std::cout << "==========================================================================\n";
        return;
    }

// 安全的除法宏
#define SAFE_DIV(a, b) ((b) == 0 ? 0.0 : (double)(a) / (b))
#define PCT(a, b) (SAFE_DIV(a, b) * 100.0)

    // 提前计算衍生数据，避免在输出流中进行过多计算
    double ipc = SAFE_DIV(commit_count, total_cycles);
    uint64_t flushed_insts = (if_total_reqs > commit_count) ? (if_total_reqs - commit_count) : 0;

    // --- 辅助格式化 Lambda 函数 (用于实现完美的横向对齐) ---
    auto fmt_str = [](const std::string &k, const std::string &v)
    {
        std::ostringstream res;
        res << std::left << std::setw(17) << k << ": " << v;
        return res.str();
    };
    auto fmt_int = [&](const std::string &k, uint64_t v)
    {
        return fmt_str(k, std::to_string(v));
    };
    auto fmt_dbl = [&](const std::string &k, double v, const std::string &suffix = "")
    {
        std::ostringstream oss;
        oss << std::fixed << std::setprecision(2) << v << suffix;
        return fmt_str(k, oss.str());
    };
    auto fmt_ipc = [&](const std::string &k, double v)
    {
        std::ostringstream oss;
        oss << std::fixed << std::setprecision(4) << v;
        return fmt_str(k, oss.str());
    };

    // 定义多列排版的列宽
    const int C1 = 35; // 第1列宽度
    const int C2 = 35; // 第2列宽度

    std::cout << "\n==========================================================================================\n";
    std::cout << "                                 CPU PERFORMANCE DASHBOARD                                \n";
    std::cout << "==========================================================================================\n";

    // ================= 模块 1: 全局 / 冲刷 / 卡顿 (横向 3 列) =================
    std::cout << std::left
              << std::setw(C1) << "[ Global Metrics ]"
              << std::setw(C2) << "[ Pipeline Flush ]"
              << "[ Stall Ratios ]\n";

    // 第 1 行
    std::cout << std::setw(C1) << fmt_int("Active Cycles", total_cycles)
              << std::setw(C2) << fmt_int("Fetch Insts", if_total_reqs)
              << fmt_dbl("IFU Stall %", PCT(ifu_stall_cycles, total_cycles), "%") << "\n";

    // 第 2 行
    std::cout << std::setw(C1) << fmt_int("Total Commits", commit_count)
              << std::setw(C2) << fmt_int("Flushed Insts", flushed_insts)
              << fmt_dbl("LSU Stall %", PCT(lsu_stall_cycles, total_cycles), "%") << "\n";

    // 第 3 行
    std::cout << std::setw(C1) << fmt_ipc("Avg IPC", ipc)
              << std::setw(C2) << fmt_dbl("Flush Rate", PCT(flushed_insts, if_total_reqs), "%")
              << "\n";

    // 第 4 行
    std::cout << std::setw(C1) << fmt_dbl("Commit Active %", PCT(commit_count, total_cycles), "%")
              << "\n";

    std::cout << "------------------------------------------------------------------------------------------\n";

    // ================= 模块 2: IFU / LSU (横向 2 列) =================
    const int C_HALF = 48; // 平分宽度
    std::cout << std::left
              << std::setw(C_HALF) << "[ Instruction Fetch (IF) ]"
              << "[ Load/Store Unit (LSU) ]\n";

    // 第 1 行
    std::cout << std::setw(C_HALF) << fmt_dbl("Fetch Hit Rate", PCT(if_hit_reqs, if_total_reqs), "%")
              << fmt_dbl("LSU Read Avg", SAFE_DIV(lsu_r_total_cycles, lsu_r_reqs), " cyc") << "\n";

    // 第 2 行
    std::cout << std::setw(C_HALF) << fmt_dbl("Avg Fetch Cyc", SAFE_DIV(if_total_cycles, if_total_reqs), " cyc")
              << fmt_dbl("LSU Write Avg", SAFE_DIV(lsu_w_total_cycles, lsu_w_reqs), " cyc") << "\n";

    // 第 3,4,5 行 (LSU 没有这么多项，留空即可)
    std::cout << std::setw(C_HALF) << fmt_dbl("Avg Hit Cyc", SAFE_DIV(if_hit_cycles, if_hit_reqs), " cyc") << "\n";
    std::cout << std::setw(C_HALF) << fmt_dbl("Avg Miss Cyc", SAFE_DIV(if_miss_cycles, if_miss_reqs), " cyc") << "\n";
    std::cout << std::setw(C_HALF) << fmt_dbl("Avg Bus Latency", SAFE_DIV(if_bus_total_cycles, if_bus_reqs), " cyc") << "\n";

    std::cout << "------------------------------------------------------------------------------------------\n";

    // ================= 模块 3: 指令级统计 (宽表) =================
    std::cout << "[ Instruction Distribution & Execution Latency ]\n";
    std::cout << std::left
              << std::setw(15) << "Type"
              << std::setw(20) << "Count"
              << std::setw(20) << "Ratio (%)"
              << "Avg Exec Cycles (if_finish -> WBU)\n";

    for (int i = 0; i < NUM_TYPES; ++i)
    {
        if (inst_counts[i] > 0)
        {
            double ratio = PCT(inst_counts[i], commit_count);
            double avg_cycles = SAFE_DIV(inst_exec_cycles[i], inst_counts[i]);

            std::cout << std::left
                      << std::setw(15) << inst_names[i]
                      << std::setw(20) << inst_counts[i]
                      << std::fixed << std::setprecision(2) << std::setw(20) << ratio
                      << std::fixed << std::setprecision(2) << avg_cycles << "\n";
        }
    }
    std::cout << "==========================================================================================\n";

// 清理宏定义，保持命名空间整洁
#undef SAFE_DIV
#undef PCT
}