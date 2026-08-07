#include <common.h>
#include "syscall.h"

static void sys_yield(Context *c)
{
  printf("Sys yield\n");
  printf("GPR1: %d, GPR2: %d, GPR3: %d, GPR4: %d\n", c->GPR1, c->GPR2, c->GPR3, c->GPR4);
  yield();
  c->GPRx = 0;
}
static void sys_exit(Context *c)
{
  printf("Sys exit\n");
  // printf("ret value: %d\n", c->GPR2);
  halt(c->GPR2);
}
static void sys_write(Context *c)
{
  int fd = c->GPR2;
  const char *buf = (const char *)c->GPR3;
  size_t len = c->GPR4;

  if (fd == 1 || fd == 2)
  {
    for (size_t i = 0; i < len; i++)
    {
      putch(buf[i]);
    }
    c->GPRx = len;
  }
  else
  {
    c->GPRx = -1;
  }
}
void do_syscall(Context *c)
{
  uintptr_t a[4];
  a[0] = c->GPR1;
  a[1] = c->GPR2;
  a[2] = c->GPR3;
  a[3] = c->GPR4;

  switch (a[0])
  {
  case SYS_yield:
    sys_yield(c);
    break;
  case SYS_exit:
    sys_exit(c);
    break;
  case SYS_write:
    sys_write(c);
    break;
  default:
    panic("Unhandled syscall ID = %d", a[0]);
  }
}
