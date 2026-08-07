#include <common.h>
#include "syscall.h"

void sys_yield(Context *c)
{
  printf("Sys yield\n");
  printf("GPR1: %d, GPR2: %d, GPR3: %d, GPR4: %d\n", c->GPR1, c->GPR2, c->GPR3, c->GPR4);
  yield();
  c->GPRx = 0;
}
void sys_exit(Context *c)
{
  printf("Sys exit\n");
  halt(c->GPR2);
}
void do_syscall(Context *c)
{
  uintptr_t a[4];
  a[0] = c->GPR1;

  switch (a[0])
  {
  case SYS_yield:
    sys_yield(c);
    break;
  case SYS_exit:
    sys_exit(c);
    break;
  default:
    panic("Unhandled syscall ID = %d", a[0]);
  }
}
