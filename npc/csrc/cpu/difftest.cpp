#include "include/difftest.h"
#include "include/CPU.h"

#define COPY_DUT_GPRS(state)                   \
    do                                         \
    {                                          \
        (state).gpr[0] = cpu->top->io_reg_0;   \
        (state).gpr[1] = cpu->top->io_reg_1;   \
        (state).gpr[2] = cpu->top->io_reg_2;   \
        (state).gpr[3] = cpu->top->io_reg_3;   \
        (state).gpr[4] = cpu->top->io_reg_4;   \
        (state).gpr[5] = cpu->top->io_reg_5;   \
        (state).gpr[6] = cpu->top->io_reg_6;   \
        (state).gpr[7] = cpu->top->io_reg_7;   \
        (state).gpr[8] = cpu->top->io_reg_8;   \
        (state).gpr[9] = cpu->top->io_reg_9;   \
        (state).gpr[10] = cpu->top->io_reg_10; \
        (state).gpr[11] = cpu->top->io_reg_11; \
        (state).gpr[12] = cpu->top->io_reg_12; \
        (state).gpr[13] = cpu->top->io_reg_13; \
        (state).gpr[14] = cpu->top->io_reg_14; \
        (state).gpr[15] = cpu->top->io_reg_15; \
    } while (0)

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

bool difftest_skip_once = false;
int step_count = 0;
bool DiffTest::step()
{
    total_step_count++;

    if (difftest_skip_once)
    {
        if (step_count++ > 0)
        {
            step_count = 0;
            difftest_skip_once = false;
        }
        dut_CPU_state.pc = cpu->top->io_nextPC;
        COPY_DUT_GPRS(dut_CPU_state); // ← 替换原来的 for 循环
        difftest_regcpy(&dut_CPU_state, DIFFTEST_TO_REF);
        return true;
    }

    // 正常比对分支
    difftest_exec(1);
    difftest_regcpy(&ref_CPU_state, DIFFTEST_TO_DUT);

    COPY_DUT_GPRS(dut_CPU_state); // ← 替换原来的 word_t* gpr + for 循环

    if (ref_CPU_state.pc != cpu->top->io_nextPC)
    {
        printf("\nDifftest(Step: %lld Cycle: %lld): nextPC mismatch: DUT=0x%08x, REF=0x%08x\n",
               total_step_count, cpu->cycle_count, cpu->top->io_nextPC, ref_CPU_state.pc);
        cpu->reg_print();
        return false;
    }

    for (int i = 0; i < REG_NUM; i++)
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
    printf(".");
    return true;
}