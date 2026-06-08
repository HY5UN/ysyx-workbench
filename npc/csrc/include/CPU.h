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
    VysyxSoCFull *top = nullptr;
    VerilatedFstC *tfp = nullptr;
    VerilatedContext *contextp = nullptr;
    DiffTest *difftest = nullptr;
    long long cycle_count = 0; 

private:
    uint64_t sim_time = 0;
};

extern CPU *cpu;

void dpic_ebreak();
void dpic_difftest_step();
void dpic_skip_difftest_once();
