#include <iostream>
#include <cstdint>
#include <cstdlib>
#include <cmath>
#include <vector>
#include <iomanip>
#include <algorithm>
#include <string>

#include "include/trace.h"
#include "include/config.h"

// 定义替换策略枚举
enum class ReplacePolicy {
    LRU,
    FIFO
};

// BTB 表项（元数据结构）
struct BTBEntry
{
    bool valid = false;
    uint32_t tag = 0;
    uint32_t age = 0; // 对于 LRU 表示最近使用距离，对于 FIFO 表示存活时间
};

// 用于保存 DSE 结果的结构体
struct SimResult 
{
    uint32_t entries;
    uint32_t assoc;
    ReplacePolicy policy; // 新增：记录该结果对应的替换策略
    double miss_rate;
    double hit_rate;

    // 重载小于号，按命中率降序排序 (命中率越高越好)
    bool operator<(const SimResult& other) const {
        return hit_rate > other.hit_rate; 
    }
};

// BTB 模拟器核心类
class BTBSimulator
{
private:
    uint32_t num_entries;   // BTB 总表项数
    uint32_t associativity; // 相联度 (Ways)
    uint32_t num_sets;      // 总 Set 数
    ReplacePolicy policy;   // 替换策略

    uint32_t offset_bits; // 块内偏移位数
    uint32_t index_bits;  // 组索引位数
    uint32_t tag_bits;    // Tag 位数
    uint32_t index_mask;  // Index 掩码

    // 二维结构：[set_index][way_index]
    std::vector<std::vector<BTBEntry>> btb;

    // 性能计数器
    uint64_t total_branches = 0;
    uint64_t miss_count = 0;
    uint64_t hit_count = 0;

public:
    BTBSimulator(uint32_t entries, uint32_t assoc, ReplacePolicy p = ReplacePolicy::LRU)
        : num_entries(entries), associativity(assoc), policy(p)
    {
        num_sets = num_entries / associativity;

        offset_bits = 2; 
        index_bits = static_cast<uint32_t>(std::log2(num_sets));
        tag_bits = 32 - index_bits - offset_bits;
        index_mask = (num_sets > 1) ? (num_sets - 1) : 0;

        btb.resize(num_sets, std::vector<BTBEntry>(associativity));
    }

    // 处理单次分支指令 PC
    void access(uint32_t pc)
    {
        total_branches++;

        uint32_t index = (pc >> offset_bits) & index_mask;
        uint32_t tag = pc >> (offset_bits + index_bits);

        auto &current_set = btb[index];
        bool is_hit = false;
        uint32_t hit_way = 0;

        // 1. 查找是否命中
        for (uint32_t w = 0; w < associativity; ++w)
        {
            if (current_set[w].valid && current_set[w].tag == tag)
            {
                is_hit = true;
                hit_way = w;
                break;
            }
        }

        if (is_hit)
        {
            hit_count++;
            
            // LRU 策略：命中的块变为最年轻（age=0），其余比它年轻的块老化
            // FIFO 策略：命中不改变存活状态（无视），因此直接跳过此段逻辑
            if (policy == ReplacePolicy::LRU)
            {
                uint32_t old_age = current_set[hit_way].age;
                for (uint32_t w = 0; w < associativity; ++w)
                {
                    if (current_set[w].valid && current_set[w].age < old_age)
                    {
                        current_set[w].age++;
                    }
                }
                current_set[hit_way].age = 0;
            }
        }
        else
        {
            miss_count++;

            // 2. 发生缺失，寻找替换槽位
            uint32_t victim_way = 0;
            bool found_invalid = false;

            // 优先寻找无效槽位
            for (uint32_t w = 0; w < associativity; ++w)
            {
                if (!current_set[w].valid)
                {
                    victim_way = w;
                    found_invalid = true;
                    break;
                }
            }

            // 若全满，寻找 age 最大的块
            // 对于 LRU，age 最大意味着最久未被使用
            // 对于 FIFO，age 最大意味着最早进入缓存
            if (!found_invalid)
            {
                uint32_t max_age = 0;
                for (uint32_t w = 0; w < associativity; ++w)
                {
                    if (current_set[w].age > max_age)
                    {
                        max_age = current_set[w].age;
                        victim_way = w;
                    }
                }
            }

            // 无论 LRU 还是 FIFO，新块进入时，组内其他所有有效块的 age 都会增加
            for (uint32_t w = 0; w < associativity; ++w)
            {
                if (current_set[w].valid)
                {
                    current_set[w].age++;
                }
            }

            // 将新分支指令录入被选中的 victim_way，重置年龄
            current_set[victim_way].valid = true;
            current_set[victim_way].tag = tag;
            current_set[victim_way].age = 0;
        }
    }

    double get_hit_rate() const
    {
        return (total_branches == 0) ? 0.0 : (static_cast<double>(hit_count) / total_branches) * 100.0;
    }

    double get_miss_rate() const
    {
        return (total_branches == 0) ? 0.0 : (static_cast<double>(miss_count) / total_branches) * 100.0;
    }
};

// ==============================================================================
// 模式：设计空间探索 (DSE)
// ==============================================================================
void run_btb_dse()
{
    if (!branchtrace_read_init())
    {
        std::cerr << "Error: 无法初始化 Branch Trace 回放输入源！" << std::endl;
        return;
    }

    // 内存缓冲 Trace，加速多组参数的遍历
    std::vector<uint32_t> trace_buffer;
    uint32_t pc = 0;
    bool is_backward = false;
    bool is_taken = false;
    
    // 仅收集分支指令的 PC
    while (branchtrace_read_next(&pc, &is_backward, &is_taken))
    {
        trace_buffer.push_back(pc);
    }
    branchtrace_read_close();

    if (trace_buffer.empty())
    {
        std::cerr << "Warning: 未读取到任何 Trace 记录。" << std::endl;
        return;
    }

    std::cout << "\n=== [BTB 设计空间探索 (DSE) 模式] ===\n";
    std::cout << "分支指令 Trace 总数: " << trace_buffer.size() << "\n\n";

    // 探索参数
    std::vector<uint32_t> btb_entries_list = {4, 8, 16, 32, 64, 128};
    std::vector<uint32_t> associativities = {1, 2, 4, 8};
    std::vector<ReplacePolicy> policies = {ReplacePolicy::LRU, ReplacePolicy::FIFO}; // 引入策略维度

    std::vector<SimResult> dse_results; // 存放扫描结果

    for (uint32_t entries : btb_entries_list)
    {
        for (uint32_t assoc : associativities)
        {
            // 过滤不合理的组合（Set 数量小于1）
            if (entries < assoc) continue;

            // 遍历并评估两种替换策略
            for (ReplacePolicy p : policies)
            {
                BTBSimulator sim(entries, assoc, p);

                for (uint32_t trace_pc : trace_buffer)
                {
                    sim.access(trace_pc);
                }

                dse_results.push_back({entries, assoc, p, sim.get_miss_rate(), sim.get_hit_rate()});
            }
        }
    }
    
    // 按命中率从高到低排序
    std::sort(dse_results.begin(), dse_results.end());

    // 打印排行榜
    std::cout << "\n=== [BTB DSE 扫描完成：按命中率性能排行榜] ===\n";
    std::cout << std::left 
              << std::setw(6)  << "Rank" 
              << std::setw(15) << "Entries(项)" 
              << std::setw(10) << "Assoc" 
              << std::setw(10) << "Policy"     // 打印时展示使用的策略
              << std::setw(15) << "Hit Rate(%)" 
              << "Miss Rate(%)\n";
    std::cout << "----------------------------------------------------------------------\n";
    
    for(size_t i = 0; i < dse_results.size(); ++i) {
        std::string policy_str = (dse_results[i].policy == ReplacePolicy::LRU) ? "LRU" : "FIFO";

        std::cout << std::left 
                  << std::setw(6)  << (i + 1)
                  << std::setw(15) << dse_results[i].entries
                  << std::setw(10) << dse_results[i].assoc
                  << std::setw(10) << policy_str
                  << std::setw(15) << std::fixed << std::setprecision(2) << dse_results[i].hit_rate
                  << std::fixed << std::setprecision(2) << dse_results[i].miss_rate << "\n";
    }
    
    std::cout << "======================================================================\n\n";
}