#include "include/difftest.h"
#include "include/CPU.h"

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

bool difftest_skip_mmio = false;
int step_count = 0;
bool DiffTest::step()
{
    total_step_count++;

    if (difftest_skip_mmio)
    {
        // printf("Skipping difftest check for MMIO access at pc 0x%08x\n", cpu->top->io_pc);
        if (step_count++ > 0) // 每条mmio指令跳过一个指令
        {
            step_count = 0;
            difftest_skip_mmio = false;
        }
        dut_CPU_state.pc = cpu->top->io_nextPC;
        for (int i = 0; i < REG_NUM; i++)
        {
            assert(i < 32);
            dut_CPU_state.gpr[i] = ((word_t *)&cpu->top->io_reg_0)[i];
        }
        difftest_regcpy(&dut_CPU_state, DIFFTEST_TO_REF);
        return true;
    }

    difftest_exec(1);
    difftest_regcpy(&ref_CPU_state, DIFFTEST_TO_DUT);

    word_t *gpr = (word_t *)&cpu->top->io_reg_0;
    if (ref_CPU_state.pc != cpu->top->io_nextPC)
    {
        printf("\nDifftest(Step: %lld Cycle: %lld): nextPC mismatch: DUT=0x%08x, REF=0x%08x\n", total_step_count, cpu->cycle_count, cpu->top->io_nextPC, ref_CPU_state.pc);
        cpu->reg_print();
        // exit(1);
        return false;
    }

    for (int i = 0; i < REG_NUM; i++)
    {
        if (gpr[i] != ref_CPU_state.gpr[i])
        {
            printf("\nDifftest(Step: %lld Cycle: %lld): GPR x%d mismatch at pc 0x%08x: DUT=0x%08x, REF=0x%08x\n", total_step_count, cpu->cycle_count, i, ref_CPU_state.pc, gpr[i], ref_CPU_state.gpr[i]);

            cpu->reg_print();
            // exit(1);
            return false;
        }
    }
    return true;
}