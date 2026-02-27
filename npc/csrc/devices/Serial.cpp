#include "DeviceIO.h"

void serial_write(int addr, int data, char wmask)
{
    if (addr == SERIAL_PORT && (wmask & 0x1))
    {
        putchar(data & 0xFF);
        fflush(stdout);
    }
}