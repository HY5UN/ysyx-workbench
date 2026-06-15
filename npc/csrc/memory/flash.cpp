#include "include/common.h"

#define FLASH_SIZE (16 * 1024 * 1024) // 16MB flash
uint32_t flash[FLASH_SIZE/4];// 16MB flash

extern "C" void flash_read(int32_t addr, int32_t *data) { 
    if(addr >= 0 && addr < FLASH_SIZE){
        *data = flash[addr/4];
    } else {
        printf("flash_read: Address out of range: 0x%08x\n", addr);
        *data = 0; // 或者其他默认值
    }


}

void init_flash()
{
    flash[0]=0x12345678; 
    flash[1]=0x9abcdef0;   
}