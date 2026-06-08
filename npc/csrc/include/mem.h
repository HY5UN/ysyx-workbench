#pragma once
#include "common.h"

extern uint8_t memory[];

int mem_read(int addr);
void mem_write(int addr, int data, char wmask);
int mem_print(uint32_t addr, int len);   

bool load_binary(const std::string &filename);

void init_rom();