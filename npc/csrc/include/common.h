#pragma once
#include<stdint.h>
#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <Vtop__Dpi.h>
#include "Vtop.h"
#include "verilated.h"
#include "include/difftest.h"   
#include "include/CPU.h"


typedef uint32_t word_t;
typedef uint32_t vaddr_t;


void sdb_mainloop(int argc, char **argv);



void ebreak();
