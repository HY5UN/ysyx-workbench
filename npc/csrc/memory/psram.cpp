#include "include/common.h"
#include "include/config.h"
#include "include/CPU.h"
#include <fstream>

#define PSRAM_SIZE (4 * 1024 * 1024) // 4MB
uint8_t psram[PSRAM_SIZE];

void init_psram(const std::string &path)
{
    //放入测试数据
    for (size_t i = 0; i < 10; ++i) {
        psram[i] = (uint8_t)(i & 0xFF); // 示例数据：每个字节等于其地址的低8位
    }

    return;
    FILE *fp = fopen(path.c_str(), "rb");
    if (fp == nullptr) {
        fprintf(stderr, "Failed to open psram image: %s\n", path.c_str());
        exit(1);
    }

    fseek(fp, 0, SEEK_END);
    long size = ftell(fp);
    fseek(fp, 0, SEEK_SET);

    if (size < 0 || (size_t)size > PSRAM_SIZE) {
        fprintf(stderr, "psram image too large: %ld bytes (max %d)\n", size, PSRAM_SIZE);
        fclose(fp);
        exit(1);
    }

    size_t n = fread(psram, 1, size, fp);
    if (n != (size_t)size) {
        fprintf(stderr, "Failed to read psram image: %s (read %zu of %ld bytes)\n", path.c_str(), n, size);
        fclose(fp);
        exit(1);
    }
    bin_size = size;

    fclose(fp);
}

extern "C" void psram_read(int addr, char *rdata)
{
    printf("psram_read: addr=0x%08x at cycle=%llu\n", addr, cpu->cycle_count);
    if ((uint32_t)addr >= PSRAM_SIZE) {
        fprintf(stderr, "psram_read: addr 0x%x out of range\n", addr);
        *rdata = 0;
        return;
    }
    *rdata = (char)psram[addr];
}

extern "C" void psram_write(int addr, char wdata)
{
    printf("psram_write: addr=0x%08x, data=0x%02x\n", addr, (uint8_t)wdata);
    if ((uint32_t)addr >= PSRAM_SIZE) {
        fprintf(stderr, "psram_write: addr 0x%x out of range\n", addr);
        return;
    }
    psram[addr] = (uint8_t)wdata;
}