#include "Vtop.h"
#include "verilated.h"
#include <iostream>
#include <vector>
#include <cstdint>
#include <fstream>

#define MEM_SIZE (64 * 1024) // 64 KB

uint8_t memory[MEM_SIZE];

void load_binary(const std::string &filename)
{
    std::ifstream file(filename, std::ios::binary);
    if (!file)
    {
        std::cerr << "Error: Could not open file " << filename << std::endl;
        exit(1);
    }

    // 获取文件大小
    file.seekg(0, std::ios::end);
    std::streamsize size = file.tellg();
    file.seekg(0, std::ios::beg);

    if (size > MEM_SIZE)
    {
        std::cerr << "Error: File size (" << size << ") is larger than memory size (" << MEM_SIZE << ")" << std::endl;
        exit(1);
    }

    // 读取文件内容直接到 memory 数组
    // memory 是 uint8_t 指针，正好对应 char 读取
    if (file.read((char *)memory, size))
    {
        std::cout << "Loaded " << size << " bytes from " << filename << " to memory." << std::endl;
    }
    else
    {
        std::cerr << "Error: Failed to read file content." << std::endl;
        exit(1);
    }
}

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
    load_binary("program.bin");

    VerilatedContext *contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    Vtop *top = new Vtop{contextp};

    reset(top, 10);
    top->io_inst = mem_read(top->io_pc);

    while (!contextp->gotFinish())
    {
        std::cout << "PC: " << std::hex << top->io_pc << std::dec
                  << " Inst: " << std::hex << top->io_inst << std::dec << std::endl;
        //打印x1和x10
        uint32_t *addr = (uint32_t *)&top->io_allReg_0;
        std::cout << "x1: " << std::hex << addr[1] << std::dec
                  << " x10: " << std::hex << addr[10] << std::dec << std::endl;


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

        

        // 临时调试
        if (top->io_pc == 0xc)
        {
            std::cout << ">>> 捕获到 Halt 信号 (PC=0xc)，仿真结束。" << std::endl;
            // 打印寄存器
            // uint32_t *addr = (uint32_t *)&top->io_allReg_0;
            // for (int i = 0; i < 32; i++)
            // {
            //     std::cout << "x" << i << ": " << std::hex << addr[i] << std::dec << std::endl;
            // }
            break;
        }
    }

    delete top;
    delete contextp;
    return 0;
}