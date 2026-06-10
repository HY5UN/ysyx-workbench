#include <am.h>
#include <riscv/riscv.h>
#include <klib-macros.h>
#include <npc.h>

extern char _heap_start;
int main(const char *args);


#define PMEM_END  0x0f001fff

Area heap = RANGE(&_heap_start, PMEM_END);
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
