#include "include/difftest.h"
#include "include/CPU.h"


CPU_State dut_CPU_state;


DiffTest::DiffTest()
{
    handle = dlopen("/home/hy5un/ysyx-workbench/nemu/build/riscv32-nemu-interpreter-so", RTLD_LAZY);
    if (!handle)
    {
        fprintf(stderr, "dlopen error: %s\n", dlerror());
        exit(1);
    }

    difftest_memcpy = (difftest_memcpy_t)dlsym(handle, "difftest_memcpy");
    difftest_regcpy = (difftest_regcpy_t)dlsym(handle, "difftest_regcpy");
    difftest_exec = (difftest_exec_t)dlsym(handle, "difftest_exec");
    difftest_init = (difftest_init_t)dlsym(handle, "difftest_init");

    if (!difftest_memcpy || !difftest_regcpy || !difftest_exec)
    {
        fprintf(stderr, "dlsym error: %s\n", dlerror());
        dlclose(handle);
        exit(1);
    }
}

DiffTest::~DiffTest()
{
    dlclose(handle);
}

bool DiffTest::step()
{
    total_step_count++;

    // 正常比对分支
    difftest_exec(1);
    difftest_regcpy(&ref_CPU_state, DIFFTEST_TO_DUT);

    // dut_CPU_state.pc 和 gpr 已由 DPI-C 在本周期更新，无需手动拷贝

    if (ref_CPU_state.pc != dut_CPU_state.pc)
    {
        printf("\nDifftest(Step: %lld Cycle: %lld): nextPC mismatch: DUT=0x%08x, REF=0x%08x\n",
               total_step_count, cpu->cycle_count, dut_CPU_state.pc, ref_CPU_state.pc);
        cpu->reg_print();
        return false;
    }

    for (int i = 0; i < 16; i++)
    {
        if (dut_CPU_state.gpr[i] != ref_CPU_state.gpr[i])
        {
            printf("\nDifftest(Step: %lld Cycle: %lld): GPR x%d mismatch at pc 0x%08x: DUT=0x%08x, REF=0x%08x\n",
                   total_step_count, cpu->cycle_count, i, ref_CPU_state.pc,
                   dut_CPU_state.gpr[i], ref_CPU_state.gpr[i]);
            cpu->reg_print();
            return false;
        }
    }

    printf("Difftest(Step: %lld Cycle: %lld): PASS at pc 0x%08x\n", total_step_count, cpu->cycle_count, dut_CPU_state.pc);
    return true;
}

void dpic_get_pc(int nextPC, int pc) {
    dut_CPU_state.pc = (word_t)nextPC;
}

void dpic_get_gprs(
    int gpr0,  int gpr1,  int gpr2,  int gpr3,
    int gpr4,  int gpr5,  int gpr6,  int gpr7,
    int gpr8,  int gpr9,  int gpr10, int gpr11,
    int gpr12, int gpr13, int gpr14, int gpr15
) {
    dut_CPU_state.gpr[0]  = (word_t)gpr0;
    dut_CPU_state.gpr[1]  = (word_t)gpr1;
    dut_CPU_state.gpr[2]  = (word_t)gpr2;
    dut_CPU_state.gpr[3]  = (word_t)gpr3;
    dut_CPU_state.gpr[4]  = (word_t)gpr4;
    dut_CPU_state.gpr[5]  = (word_t)gpr5;
    dut_CPU_state.gpr[6]  = (word_t)gpr6;
    dut_CPU_state.gpr[7]  = (word_t)gpr7;
    dut_CPU_state.gpr[8]  = (word_t)gpr8;
    dut_CPU_state.gpr[9]  = (word_t)gpr9;
    dut_CPU_state.gpr[10] = (word_t)gpr10;
    dut_CPU_state.gpr[11] = (word_t)gpr11;
    dut_CPU_state.gpr[12] = (word_t)gpr12;
    dut_CPU_state.gpr[13] = (word_t)gpr13;
    dut_CPU_state.gpr[14] = (word_t)gpr14;
    dut_CPU_state.gpr[15] = (word_t)gpr15;
}