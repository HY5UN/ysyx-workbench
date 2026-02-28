#pragma once
#include "common.h"
#include "Vtop.h"
#include "verilated.h"
#include <Vtop__Dpi.h>

class CPU
{
public:
    CPU(int argc, char **argv);
    ~CPU();

    void reg_print();
    void reset(int n);
    void execute(uint64_t steps);
    void execute_once();

private:
    VerilatedContext *contextp = nullptr;
    Vtop *top = nullptr;
};

void ebreak();
