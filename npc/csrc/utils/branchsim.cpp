#include <cstdio>
#include <cstdint>
#include "include/trace.h"

void simulate_branch_predictors() {
    printf("========================================================\n");
    printf("[BranchSim] Initializing static branch predictors...\n");

    if (!branchtrace_read_init()) {
        printf("[BranchSim] Error: Failed to open branch trace file.\n");
        return;
    }

    // 统计计数器
    uint64_t total_branches = 0;
    uint64_t correct_always_taken = 0;
    uint64_t correct_always_not_taken = 0;
    uint64_t correct_btfnt = 0;

    bool is_backward = false;
    bool is_taken = false;
    uint32_t pc = 0;

    // 逐条读取 Trace，进行预测并校验
    while (branchtrace_read_next(&pc, &is_backward, &is_taken)) {
        total_branches++;

        // 策略 1: Always Taken (永远预测跳转)
        if (is_taken == true) {
            correct_always_taken++;
        }

        // 策略 2: Always Not Taken (永远预测不跳转)
        if (is_taken == false) {
            correct_always_not_taken++;
        }

        // 策略 3: BTFNT (Backward Taken, Forward Not Taken)
        // 向后跳转预测为 Taken (true)，向前跳转预测为 Not Taken (false)
        bool pred_btfnt = is_backward; 
        if (pred_btfnt == is_taken) {
            correct_btfnt++;
        }
    }

    branchtrace_read_close();

    // 打印最终统计结果
    if (total_branches == 0) {
        printf("[BranchSim] No branch instructions found in the trace.\n");
        printf("========================================================\n");
        return;
    }

    // 计算准确率
    double acc_at    = (double)correct_always_taken / total_branches * 100.0;
    double acc_ant   = (double)correct_always_not_taken / total_branches * 100.0;
    double acc_btfnt = (double)correct_btfnt / total_branches * 100.0;

    printf("[BranchSim] Simulation finished. Total Branches: %llu\n\n", (unsigned long long)total_branches);
    printf("%-25s %-15s %-15s\n", "Strategy", "Correct Preds", "Accuracy");
    printf("--------------------------------------------------------\n");
    printf("%-25s %-15llu %6.2f %%\n", "Always Taken",     (unsigned long long)correct_always_taken,     acc_at);
    printf("%-25s %-15llu %6.2f %%\n", "Always Not Taken", (unsigned long long)correct_always_not_taken, acc_ant);
    printf("%-25s %-15llu %6.2f %%\n", "BTFNT",            (unsigned long long)correct_btfnt,            acc_btfnt);
    printf("========================================================\n");
}