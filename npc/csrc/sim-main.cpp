#include "Vtop.h"
#include "verilated.h"
#include <iostream>
#include <vector>
#include <cstdint>

#define MEM_SIZE (64 * 1024) // 64 KB

uint8_t memory[MEM_SIZE];

uint32_t mem_read(uint32_t addr)
{
    if (addr + 3 >= MEM_SIZE)
    {
        std::cerr << "Memory read out of bounds: " << std::hex << addr << std::dec << std::endl;
        return 0;
    }
    return memory[addr] | (memory[addr + 1] << 8) | (memory[addr + 2] << 16) | (memory[addr + 3] << 24);
}

void mem_write(uint32_t addr, uint32_t data)
{
    if (addr + 3 >= MEM_SIZE)
    {
        std::cerr << "Memory write out of bounds: " << std::hex << addr << std::dec << std::endl;
        return;
    }
    memory[addr] = data & 0xFF;
    memory[addr + 1] = (data >> 8) & 0xFF;
    memory[addr + 2] = (data >> 16) & 0xFF;
    memory[addr + 3] = (data >> 24) & 0xFF;
}

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
        top->clock = 1;
        top->eval();
        top->io_inst = mem_read(top->io_pc);
        if (top->io_to_mem_wen)
        {
            mem_write(top->io_to_mem_addr, top->io_to_mem_wdata);
        }
        top->io_to_mem_rdata = mem_read(top->io_to_mem_addr);
        top->eval();

        top->clock = 0;
        top->eval();
        contextp->timeInc(1);
    }

    delete top;
    delete contextp;
    return 0;
}