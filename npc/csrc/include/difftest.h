#pragma once

#include "include/common.h"
#include <dlfcn.h>
#include <include/config.h>

enum
{
    DIFFTEST_TO_DUT,
    DIFFTEST_TO_REF
};

typedef void (*difftest_memcpy_t)(uint32_t addr, void *buf, size_t n, bool direction);
typedef void (*difftest_regcpy_t)(void *dut, bool direction);
typedef void (*difftest_exec_t)(uint64_t n);
typedef void (*difftest_init_t)(void *dut);

struct CPU_State
{
    word_t gpr[32];
    word_t pc;
    word_t mepc;
    word_t mstatus;
    word_t mcause;
    word_t mtvec;
};

class DiffTest
{
public:
    DiffTest();
    ~DiffTest();
    difftest_memcpy_t difftest_memcpy;
    difftest_regcpy_t difftest_regcpy;
    difftest_exec_t difftest_exec;
    difftest_init_t difftest_init;
    bool step();

    int steps_after_mismatch = STEPS_AFTER_MISMATCH;
    bool in_mismatch = false;

private:
    long long total_step_count = 0;

    void *handle;
    CPU_State ref_CPU_state;
};

extern CPU_State dut_CPU_state;

void dpic_save_cpu_state(int nextPC, int pc,int inst);
void dpic_save_gprs(
    int gpr0, int gpr1, int gpr2, int gpr3,
    int gpr4, int gpr5, int gpr6, int gpr7,
    int gpr8, int gpr9, int gpr10, int gpr11,
    int gpr12, int gpr13, int gpr14, int gpr15);