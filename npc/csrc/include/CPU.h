#include "include/common.h"
#include "include/difftest.h"

class CPU
{
public:
    CPU(int argc, char **argv);
    ~CPU();

    void reg_print();
    void reset(int n);
    bool execute(uint64_t steps);
    bool execute_once();
    Vtop *top = nullptr;
    VerilatedFstC *tfp = nullptr;
    VerilatedContext *contextp = nullptr;
    DiffTest *difftest = nullptr;

private:
    uint64_t sim_time = 0;
};

extern CPU *cpu;