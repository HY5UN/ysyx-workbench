#include "include/common.h"
#include "include/CPU.h"
void dpic_putch(char c)
{
    if(c == '\n')
    {
        printf ("       @pc: 0x%08x, cycle: %llu, ×2=%llu", cpu->pc, (unsigned long long)cpu->cycle_count,(unsigned long long)cpu->cycle_count*2);
    }
    putc(c);

}D