#include "include/common.h"
#include "include/difftest.h"   


class CPU
{
public:
    CPU(int argc, char **argv);
    ~CPU();

    void reg_print();
    void reset(int n);
    void execute(uint64_t steps);
    void execute_once();
    Vtop *top = nullptr;
    VerilatedContext *contextp = nullptr;
    DiffTest *difftest = nullptr;
};

extern CPU *cpu;