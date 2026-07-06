#include <iostream>
#include <cstdint>
#include <cstdlib>
#include <cmath>
#include <vector>
#include <iomanip>
#include <algorithm>

#include "include/trace.h"
#include "include/config.h"



// BTB 表项（元数据结构）
struct BTBEntry
{
    bool valid = false;
    uint32_t tag = 0;
    uint32_t age = 0; // 用于 LRU 替换策略的计数器
    // 真实的 BTB 这里还会存 target_pc 和 预测状态机，
    // 但仅评估命中率的话，有 tag 就足够了。
};

// 用于保存 DSE 结果的结构体
struct SimResult 
{
    uint32_t entries;
    uint32_t assoc;
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

    uint32_t offset_bits; // 块内偏移位数 (固定指令对齐)
    uint32_t index_bits;  // 组索引位数
    uint32_t tag_bits;    // Tag 位数

    uint32_t index_mask; // Index 掩码

    // 二维结构：[set_index][way_index]
    std::vector<std::vector<BTBEntry>> btb;

    // 性能计数器
    uint64_t total_branches = 0;
    uint64_t miss_count = 0;
    uint64_t hit_count = 0;

public:
    BTBSimulator(uint32_t entries, uint32_t assoc)
        : num_entries(entries), associativity(assoc)
    {
        num_sets = num_entries / associativity;

        // 指令地址通常是 4 字节对齐的，忽略最低两位
        offset_bits = 2; 
        index_bits = static_cast<uint32_t>(std::log2(num_sets));
        tag_bits = 32 - index_bits - offset_bits;

        // 构造掩码
        index_mask = (num_sets > 1) ? (num_sets - 1) : 0;

        // 初始化存储阵列
        btb.resize(num_sets, std::vector<BTBEntry>(associativity));
    }

    // 处理单次分支指令 PC
    void access(uint32_t pc)
    {
        total_branches++;

        // 提取索引和 Tag
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
            // 更新 LRU 状态：命中的变为最年轻（age=0），其他比它年轻的加 1
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

            // 若全满，执行 LRU 策略寻找 age 最大的块
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

            // 升级当前组内所有有效块的 age
            for (uint32_t w = 0; w < associativity; ++w)
            {
                if (current_set[w].valid)
                {
                    current_set[w].age++;
                }
            }

            // 将新分支指令录入被选中的 victim_way
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

    void print_summary() const
    {
        std::cout << "--------------------------------------------------\n";
        std::cout << "Entries: " << std::setw(4) << num_entries << " | "
                  << "Assoc: " << associativity << "-Way\n";
        std::cout << "[Tag: " << tag_bits << " bits | Idx: " << index_bits << " bits | Off: " << offset_bits << " bits]  \n";

        std::cout << "Total Branches: " << total_branches << " | Hits: " << hit_count << " | Misses: " << miss_count << "\n";
        std::cout << std::fixed << std::setprecision(2)
                  << "Hit Rate: " << get_hit_rate() << " % | Miss Rate: " << get_miss_rate() << " %\n";
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

    // 探索不同的 BTB 容量（项数）与相联度
    std::vector<uint32_t> btb_entries_list = {4, 8, 16, 32, 64, 128};
    std::vector<uint32_t> associativities = {1, 2, 4, 8};

    std::vector<SimResult> dse_results; // 存放扫描结果

    for (uint32_t entries : btb_entries_list)
    {
        for (uint32_t assoc : associativities)
        {
            // 过滤不合理的组合（Set 数量小于1）
            if (entries < assoc)
                continue;

            BTBSimulator sim(entries, assoc);

            for (uint32_t trace_pc : trace_buffer)
            {
                sim.access(trace_pc);
            }

            // 将结果推入记录中用于后续排行
            dse_results.push_back({entries, assoc, sim.get_miss_rate(), sim.get_hit_rate()});
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
              << std::setw(15) << "Hit Rate(%)" 
              << "Miss Rate(%)\n";
    std::cout << "--------------------------------------------------------------\n";
    
    for(size_t i = 0; i < dse_results.size(); ++i) {
        std::cout << std::left 
                  << std::setw(6)  << (i + 1)
                  << std::setw(15) << dse_results[i].entries
                  << std::setw(10) << dse_results[i].assoc
                  << std::setw(15) << std::fixed << std::setprecision(2) << dse_results[i].hit_rate
                  << std::fixed << std::setprecision(2) << dse_results[i].miss_rate << "\n";
    }
    
    std::cout << "==============================================================\n\n";
}
