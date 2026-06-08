
#include <iostream>
#include <vector>
#include <cstdint>
#include <fstream>

uint32_t rom_begin = 0x20000000;
uint32_t rom_end = 0x20000fff;
uint32_t rom[4096 / 4];

void init_rom()
{
    rom[0] = 0x00100073; // ebreak
}

extern "C" void mrom_read(int32_t addr, int32_t *data)
{
    
    addr &= 0xfffffffc;
    if (addr >= rom_begin && addr < rom_end)
    {
        *data = rom[(addr - rom_begin) / 4];
    }
    else
    {
        std::cerr << "mrom_read: Address out of range: " << std::hex << addr << std::dec << std::endl;
        *data = 0; // 或者其他默认值
    }

    printf("mrom_read: addr=0x%08x, data=0x%08x\n", addr, *data);
}