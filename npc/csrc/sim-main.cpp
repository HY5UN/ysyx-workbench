#include "Vtop.h"
#include "verilated.h"




void reset(Vtop *top, int n)
{
    top->reset = 1;
    for (int i = 0; i < n; i++)
    {
        top->clock = 0;
        top->eval();
        top->clock = 1;
        top->eval();
    }
    top->reset = 0;
    top->clock = 0;
    top->eval();
}
int main(int argc, char **argv)
{
    VerilatedContext *contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    Vtop *top = new Vtop{contextp};

    reset(top, 10);

    while (!contextp->gotFinish())
    {
        top->clock = !top->clock;
        top->eval();
        contextp->timeInc(1);
    }

    delete top;
    delete contextp;
    return 0;
}