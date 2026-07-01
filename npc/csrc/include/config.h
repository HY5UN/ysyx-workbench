#pragma once

// #define ENABLE_DIFFTEST
#define STEPS_AFTER_MISMATCH 10

// #define ENABLE_ITRACE
// #define ENABLE_MTRACE
// #define MTRACE_ONLY
#define ITRACE_MAX_LINES 5000

// #define ENABLE_FTRACE
#define FTRACE_MAX_LINES 5000

#define ENABLE_FST // 查看波形命令 gtkwave waveform.fst
#define MAX_SIM_TIME 10000
#define FST_TRACE_LATEST false
#define FST_START_TIME 10000 //如果记录时钟下降沿，需要乘以2

// #define ENABLE_SDB

// #define RECORD_PCTRACE

// #define RUN_CACHESIM 
#define DSE_MODE
#define PARA_R 9.9
#define TARGET_CACHE_SIZE_B 64
#define TARGET_BLOCK_SIZE_B 4
#define TARGET_ASSOC 8