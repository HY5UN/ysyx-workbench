#include "Vtop.h"
#include "verilated.h"
#include <iostream>
#include <vector>
#include <cstdint>
#include <fstream>
#include <Vtop__Dpi.h>
#include "minirv.cpp"



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

int mem_print(int addr = BEGIN_ADDR, int len = 64)
{
    uint32_t u_addr = (uint32_t)addr;
    u_addr &= ~0x3;
    if ((u_addr + len - 1) >= MEM_SIZE)
    {
        std::cerr << "Memory read out of bounds: " << std::hex << u_addr << std::dec << std::endl;
        return 0;
    }
    std::cout << "Memory dump from " << std::hex << u_addr << " to " << (u_addr + len - 1) << ":" << std::dec;

    u_addr -= BEGIN_ADDR; // 转换为 memory 数组的索引
    for (int i = 0; i < len; i += 4)
    {
        if (i % 16 == 0)
        {
            std::cout << std::endl
                      << std::hex << (u_addr + i) << ": ";
        }
        // 打印4字节，从高地址到低地址
        for (int j = 3; j >= 0; --j)
        {
            printf("%02x", memory[u_addr + i + j]);
        }
        printf(" ");
    }
    std::cout << std::dec << std::endl;
    return 0;
}

void reg_print(Vtop *top)
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



int main(int argc, char **argv)
{
    if (argc > 1)
    {
        load_binary(argv[1]);
    }
    else
    {
        const std::string default_bin = "resource/sum.bin";
        std::cout << "No binary file provided. Loading default:" << default_bin << std::endl;
        load_binary(default_bin);
    }
    CorrectSimulator *correct_simulator = new CorrectSimulator((void *)memory);

    VerilatedContext *contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    Vtop *top = new Vtop{contextp};

    reset(top, 10);

    while (!contextp->gotFinish())
    {
        // std::cout << "\nPC: " << std::hex << top->io_pc << std::dec
        //           << " Inst: " << std::hex << top->io_inst << std::dec << std::endl;

        top->clock = 1;
        top->eval();

        top->clock = 0;
        top->eval();
        contextp->timeInc(1);

        // reg_print(top);
        // mem_print(0, 128);
        // std::cin.get();

        // correct_simulator->inst_cycle();

        // if (!correct_simulator->compare(top))
        // {
        //     reg_print(top);
        //     mem_print(prev_mem_addr - 64, 64);

        //     std::cerr << "Mismatch detected. Exiting simulation." << std::endl;
        //     std::cin.get();
        // }

        if (ebreak_triggered)
        {
            if (top->io_allReg_10 == 0)
            {
                std::cout << "HIT GOOD TRAP!" << std::endl;
            }
            else
            {
                reg_print(top);

                std::cout << "HIT BAD TRAP! x10 = " << std::hex << top->io_allReg_10 << std::dec << std::endl;
            }

            break;
        }
    }

    delete top;
    delete contextp;
    return 0;
}


void ebreak()
{
    std::cout << ">>> 执行 ebreak 指令，触发仿真结束。" << std::endl;
    ebreak_triggered = true;
}