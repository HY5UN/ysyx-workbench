#include <am.h>
#include <riscv/riscv.h>
#include <klib-macros.h>
#include <npc.h>

extern char _heap_start;
// extern char _heap_end;
int main(const char *args);

#define SRAM_START 0x0f000000
#define SRAM_END  0x0f001fff

Area heap = RANGE(&_heap_start, SRAM_END);
static const char mainargs[MAINARGS_MAX_LEN] = TOSTRING(MAINARGS_PLACEHOLDER); // defined in CFLAGS

void putch(char ch) {
  outl(SERIAL_PORT, ch);
}

void halt(int code) {

  asm volatile("mv a0, %0" : : "r"(code));

  asm volatile("ebreak");

  // should not reach here
  while (1);
}

void _trm_init() {
  int ret = main(mainargs);
  halt(ret);
}
