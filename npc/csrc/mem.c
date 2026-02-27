#include "mem.h"

#define MEM_SIZE (1024 * 1024)
#define BEGIN_ADDR 0x80000000
uint8_t memory[MEM_SIZE];
uint32_t prev_mem_addr = 0;


int mem_read(int addr)
{
    uint32_t u_addr = (uint32_t)addr;
    u_addr &= ~0x3;
    prev_mem_addr = u_addr;
    u_addr -= BEGIN_ADDR;
    // printf("Reading to memory: addr= %08x, translated addr= %08x\n", addr, u_addr);
    if ((u_addr + 3) >= MEM_SIZE)
    {
        // std::cerr << "Memory read out of bounds: " << std::hex << u_addr << std::dec << std::endl;
        //  std::cin.get();
        return 0;
    }
    return memory[u_addr] | (memory[u_addr + 1] << 8) | (memory[u_addr + 2] << 16) | (memory[u_addr + 3] << 24);
}

void mem_write(int addr, int data, char wmask)
{
    uint32_t u_addr = (uint32_t)addr;
    u_addr &= ~0x3;
    prev_mem_addr = u_addr;
    // printf("Writing to memory: addr=%08x data=%08x wmask=%02x\n", addr, data, (int)wmask);

    u_addr -= BEGIN_ADDR; // 转换为 memory 数组的索引
    if ((u_addr + 3) >= MEM_SIZE)
    {
        std::cerr << "Memory write out of bounds: " << std::hex << u_addr << std::dec << std::endl;
        std::cin.get();
        return;
    }
    memory[u_addr] = (wmask & 0x1) ? (data & 0xFF) : memory[u_addr];
    memory[u_addr + 1] = (wmask & 0x2) ? ((data >> 8) & 0xFF) : memory[u_addr + 1];
    memory[u_addr + 2] = (wmask & 0x4) ? ((data >> 16) & 0xFF) : memory[u_addr + 2];
    memory[u_addr + 3] = (wmask & 0x8) ? ((data >> 24) & 0xFF) : memory[u_addr + 3];
}
