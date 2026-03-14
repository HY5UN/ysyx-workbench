#include "include/difftest.h"

DiffTest::DiffTest()  {
    handle = dlopen("../nemu/build/obj-riscv32-nemu-interpreter-so", RTLD_LAZY);
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