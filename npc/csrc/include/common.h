#pragma once
#include<stdint.h>
#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <Vtop__Dpi.h>
#include "Vtop.h"
#include "verilated.h"

#define ENABLE_ITRACE
#define ENABLE_MTRACE
typedef uint32_t word_t;
typedef uint32_t vaddr_t;

void sdb_mainloop(int argc, char **argv);

class CPU
{
public:
    CPU(int argc, char **argv);
    ~CPU();

    void reg_print();
    void reset(int n);
    void execute(uint64_t steps);
    void execute_once();
    Vtop *top = nullptr;
    VerilatedContext *contextp = nullptr;
};

extern CPU *cpu;

void ebreak();
