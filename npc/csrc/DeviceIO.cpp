#include "include/DeviceIO.h"

struct Device

{
    uint32_t begin_addr;
    uint32_t end_addr;
    uint32_t (*read)(int addr);
    void (*write)(int addr, int data, char wmask);
};

Device devices[] = {
    {}
};