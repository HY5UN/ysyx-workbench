#pragma once
#include "common.h"

int mem_read(int addr);
void mem_write(int addr, int data, char wmask);
int mem_print(uint32_t addr, int len);   

void load_binary(const std::string &filename);
