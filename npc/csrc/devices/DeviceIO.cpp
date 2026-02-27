#include "include/DeviceIO.h"

struct Device

{
    uint32_t begin_addr;
    int len;
    uint32_t (*read)(int addr);
    void (*write)(int addr, int data, char wmask);
};

Device devices[] = {
    {SERIAL_PORT, 8, nullptr, serial_write}
};

bool handle_mmio_write(int addr, int data, char wmask)
{
    for (const auto &dev : devices)
    {
        if (addr >= dev.begin_addr && addr < dev.begin_addr + dev.len)
        {
            if (dev.write)
            {
                dev.write(addr, data, wmask);
                return true;
            }
        }
    }
    return false;
}

bool handle_mmio_read(int addr, int &data)
{
    for (const auto &dev : devices)
    {
        if (addr >= dev.begin_addr && addr < dev.begin_addr + dev.len)
        {
            if (dev.read)
            {
                data = dev.read(addr);
                return true;
            }
        }
    }
    return false;
}