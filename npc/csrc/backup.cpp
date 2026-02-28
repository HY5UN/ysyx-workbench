#include "Vtop.h"
#include "verilated.h"
#include <iostream>
#include <vector>
#include <cstdint>
#include <fstream>
#include <Vtop__Dpi.h>
#include "minirv.cpp"
#include "include/mem.h"
#include "include/DeviceIO.h"


bool ebreak_triggered = false;



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
    //CorrectSimulator *correct_simulator = new CorrectSimulator((void *)memory);

    init_devices();

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