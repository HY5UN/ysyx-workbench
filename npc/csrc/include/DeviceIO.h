#pragma once
#include "common.h"

#define SERIAL_PORT 0x10000000
#define RTC         0x10000004
#define RTC_UPTIME 0x10000028

void add_device(uint32_t begin_addr, int len, uint32_t (*read)(int addr), void (*write)(int addr, int data, char wmask));
bool handle_mmio_write(int addr, int data, char wmask);
bool handle_mmio_read(int addr, int &data);


void init_devices();

void init_serial();
void init_timer();