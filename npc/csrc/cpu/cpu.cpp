#include "common.h"

static bool ebreak_triggered = false;

CPU::CPU(int argc, char **argv)
{
    contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    top = new Vtop{contextp};
}

CPU::~CPU()
{
    delete top;
    delete contextp;
}

void CPU::reg_print()
{
    // 打印寄存器 每行8个寄存器
    uint32_t *addr = (uint32_t *)&top->io_allReg_0;
    for (int i = 0; i < 16; i++)
    {
        if (i % 8 == 0 && i != 0)
            printf("\n");
        printf("\tx%-2d: %04x ", i, addr[i]);
    }
    printf("\n");
}

void CPU::reset(int n)
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

void CPU::execute(uint64_t steps)
{
    for (; steps > 0 && !contextp->gotFinish(); steps--)
    {
        execute_once();
    }
}

void CPU::execute_once()
{
    top->clock = 0;
    top->eval();
    top->clock = 1;
    top->eval();
    contextp->timeInc(1);
    if (ebreak_triggered)
    {
        if (top->io_allReg_10 == 0)
        {
            std::cout << "HIT GOOD TRAP!" << std::endl;
        }
        else
        {
            reg_print();

            std::cout << "HIT BAD TRAP! x10 = " << std::hex << top->io_allReg_10 << std::dec << std::endl;
        }
    }
}

void ebreak()
{
    std::cout << ">>> 执行 ebreak 指令，触发仿真结束。" << std::endl;
    ebreak_triggered = true;
}