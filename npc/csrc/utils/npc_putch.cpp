#include "include/common.h"
#include "include/CPU.h"
void dpic_putch(char c)
{
    putchar(c);
    if(c == '\n')
    {
        printf ("[cycle: %llu, ×2= %llu]     ", (unsigned long long)cpu->cycle_count,(unsigned long long)cpu->cycle_count*2);
    }

}