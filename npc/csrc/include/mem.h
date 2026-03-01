#pragma once
#include "common.h"

int mem_read(int addr);
void mem_write(int addr, int data, char wmask);
int mem_print(uint32_t addr, int len);   

bool load_binary(const std::string &filename);

// Raw memory access for difftest initialization
uint8_t *get_raw_memory();
uint32_t get_raw_mem_base();
size_t get_raw_mem_size();
size_t get_loaded_img_size();
