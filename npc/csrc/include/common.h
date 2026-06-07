#pragma once
#include<stdint.h>
#include <iostream>
#include <fstream>
#include <vector>
#include <string>
#include <Vysyx_26010036__Dpi.h>
#include "Vysyx_26010036.h"
#include "verilated.h"


#define BEGIN_ADDR 0x80000000
#define REG_NUM 16
extern long long bin_size;  


typedef uint32_t word_t;
typedef uint32_t vaddr_t;


void sdb_mainloop(int argc, char **argv);




