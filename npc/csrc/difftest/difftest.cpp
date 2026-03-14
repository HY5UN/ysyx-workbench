#include "include/difftest.h"
#include "include/CPU.h"

DiffTest::DiffTest()  {
    handle = dlopen("/home/hy5un/ysyx-workbench/nemu/build/riscv32-nemu-interpreter-so", RTLD_LAZY);
    if (!handle) {
        fprintf(stderr, "dlopen error: %s\n", dlerror());
        exit(1);
    }

    difftest_memcpy = (difftest_memcpy_t)dlsym(handle, "difftest_memcpy");
    difftest_regcpy = (difftest_regcpy_t)dlsym(handle, "difftest_regcpy");
    difftest_exec   = (difftest_exec_t)dlsym(handle, "difftest_exec");
    difftest_init   = (difftest_init_t)dlsym(handle, "difftest_init");

    if (!difftest_memcpy || !difftest_regcpy || !difftest_exec) {
        fprintf(stderr, "dlsym error: %s\n", dlerror());
        dlclose(handle);
        exit(1);
    }

}


DiffTest::~DiffTest() {
    dlclose(handle);

}

bool difftest_mmio_skip=false;

void DiffTest::step() {

    //temp
    if(cpu->top->io_pc==0x80036160){
        printf("current difftest_mmio_skip:%d", difftest_mmio_skip);
    }
    else if(cpu->top->io_pc==0x80036164){
        printf("current difftest_mmio_skip:%d", difftest_mmio_skip);
    }

    if(difftest_mmio_skip)
    {
        difftest_mmio_skip=false;
        dut_CPU_state.pc = cpu->top->io_pc;
        for (int i = 0; i < REG_NUM; i++) {
            assert(i < 32);
            dut_CPU_state.gpr[i] = ((word_t *)&cpu->top->io_allReg_0)[i];
        }
        difftest_regcpy(&dut_CPU_state, DIFFTEST_TO_REF);
        return;
    }


    difftest_exec(1);
    difftest_regcpy(&ref_CPU_state, DIFFTEST_TO_DUT);

    word_t *gpr = (word_t *)&cpu->top->io_allReg_0;

    for (int i = 0; i < REG_NUM; i++) {
        if (gpr[i] != ref_CPU_state.gpr[i]) {
            printf("GPR x%d mismatch at pc 0x%08x: DUT=0x%08x, REF=0x%08x\n", i, ref_CPU_state.pc, gpr[i], ref_CPU_state.gpr[i]);

            cpu->reg_print();
            exit(1);
        }
    }

}