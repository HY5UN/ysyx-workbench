#ifndef __MEM_H__
#define __MEM_H__
#include "common.h"

int mem_read(int addr);
void mem_write(int addr, int data, char wmask);
int mem_print(int addr, int len);   

#endif