#include "include/common.h"
#include "include/config.h"
#include "include/CPU.h"
#include <fstream>

#define SDRAM_SIZE (32 * 1024 * 1024) // 32 MB
uint16_t sdram[SDRAM_SIZE / 2];

extern "C" void sdram_read(int addr, int16_t *rdata)
{
    if ((uint32_t)addr >= SDRAM_SIZE / 2)
    {
        fprintf(stderr, "[NPC] sdram_read: addr 0x%x out of range\n", addr);
        *rdata = 0;
        return;
    }
    *rdata = sdram[addr];
    // printf(".");
    printf("[NPC] sdram_read: addr=0x%08x, data=0x%04x, at cycle=%llu\n", addr, (uint16_t)*rdata, cpu->cycle_count);
}

extern "C" void sdram_write(int addr, int16_t wdata, char dqm)
{
    // printf(".");
    printf("[NPC] sdram_write: addr=0x%08x, data=0x%04x, dqm=0x%02x, at cycle=%llu\n", addr, (uint16_t)wdata, (uint8_t)dqm, cpu->cycle_count);
    if ((uint32_t)addr >= SDRAM_SIZE / 2)
    {
        fprintf(stderr, "[NPC] sdram_write: addr 0x%x out of range\n", addr);
        return;
    }

    if (dqm & 0x3 == 0)
    {
        sdram[addr] = wdata;
        return;
    }
    if (dqm & 0x3 == 0x1)
    {
        sdram[addr] = (sdram[addr] & 0x00FF) | (wdata & 0xFF00);
        return;
    }
    if (dqm & 0x3 == 0x2)
    {
        sdram[addr] = (sdram[addr] & 0xFF00) | (wdata & 0x00FF);
        return;
    }
}