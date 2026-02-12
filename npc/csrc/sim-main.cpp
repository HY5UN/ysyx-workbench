#include "Vtop.h"
#include "verilated.h"
#include <iostream>
#include <vector>
#include <cstdint>
#include <fstream>
#include <Vtop__Dpi.h>

#define MEM_SIZE (64*1024 * 1024) // 64 MB

uint8_t memory[MEM_SIZE];
bool ebreak_triggered = false;

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
    load_binary("resource/mem.bin");

    VerilatedContext *contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    Vtop *top = new Vtop{contextp};

    reset(top, 10);

    uint32_t prev_a0 =0;

    while (!contextp->gotFinish() && !ebreak_triggered)
    {
        // std::cout << "PC: " << std::hex << top->io_pc << std::dec
        //           << " Inst: " << std::hex << top->io_inst << std::dec << std::endl;
        

        top->clock = 1;
        top->eval();

        top->clock = 0;
        top->eval();
        contextp->timeInc(1);


        //std::cin.get();
        

        //调试
        if (prev_a0 != top->io_allReg_10) {
            std::cout << "a0 changed: " << std::hex << top->io_allReg_10 << std::dec << std::endl;
            //打印当前pc和指令
            std::cout << "Current PC: " << std::hex << top->io_pc << std::dec ;
            std::cout << "  Current instruction: " << std::hex << top->io_inst << std::dec << std::endl;
            prev_a0 = top->io_allReg_10;
        }
    }
    // 打印寄存器 每行8个寄存器
    uint32_t *addr = (uint32_t *)&top->io_allReg_0;
    for (int i = 0; i < 16; i++)
    {
        if (i % 8 == 0 && i != 0)
            printf("\n");
        printf("\tx%-2d: %04x ", i, addr[i]);
    }
    printf("\n");

    delete top;
    delete contextp;
    return 0;
}

int  mem_read(int addr)
{
    addr &= ~0x3; 
    if (addr + 3 >= MEM_SIZE)
    {
        std::cerr << "Memory read out of bounds: " << std::hex << addr << std::dec << std::endl;
        return 0;
    }
    return memory[addr] | (memory[addr + 1] << 8) | (memory[addr + 2] << 16) | (memory[addr + 3] << 24);
}

void mem_write(int addr,  int data, char wmask) 
{
    addr &= ~0x3; 

    if (addr + 3 >= MEM_SIZE)
    {
        std::cerr << "Memory write out of bounds: " << std::hex << addr << std::dec << std::endl;
        return;
    }
    memory[addr] = (wmask & 0x1) ? (data & 0xFF) : memory[addr];
    memory[addr + 1] = (wmask & 0x2) ? ((data >> 8) & 0xFF) : memory[addr + 1];
    memory[addr + 2] = (wmask & 0x4) ? ((data >> 16) & 0xFF) : memory[addr + 2];
    memory[addr + 3] = (wmask & 0x8) ? ((data >> 24) & 0xFF) : memory[addr + 3];
}

void ebreak()
{
    std::cout << ">>> 执行 ebreak 指令，触发仿真结束。" << std::endl;
    ebreak_triggered = true;
}