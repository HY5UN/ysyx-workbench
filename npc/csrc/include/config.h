#pragma once

#define ENABLE_DIFFTEST
#define STEPS_AFTER_MISMATCH 10

// #define ENABLE_ITRACE
// #define ENABLE_FTRACE

#define ENABLE_FST // 查看波形命令 gtkwave waveform.fst
#define MAX_SIM_TIME 10000
#define FST_TRACE_LATEST false
#define FST_START_TIME 0 //如果记录时钟下降沿，需要乘以2

// #define ENABLE_SDB

