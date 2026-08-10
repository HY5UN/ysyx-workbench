#include <common.h>
#include <memory.h>
#include <fs.h>
#include <sys/time.h>
#include "syscall.h"
#include <proc.h>

static void sys_yield(Context *c)
{
  printf("Sys yield\n");
  printf("GPR1: %d, GPR2: %d, GPR3: %d, GPR4: %d\n", c->GPR1, c->GPR2, c->GPR3, c->GPR4);
  yield();
  c->GPRx = 0;
}

static void sys_open(Context *c)
{
  c->GPRx = fs_open((const char *)c->GPR2, c->GPR3, c->GPR4);
}
static void sys_read(Context *c)
{
  c->GPRx = fs_read(c->GPR2, (void *)c->GPR3, c->GPR4);
}
static void sys_write(Context *c)
{
  c->GPRx = fs_write(c->GPR2, (const void *)c->GPR3, c->GPR4);
}
static void sys_close(Context *c)
{
  c->GPRx = fs_close(c->GPR2);
}
static void sys_lseek(Context *c)
{
  c->GPRx = fs_lseek(c->GPR2, c->GPR3, c->GPR4);
}
static void sys_brk(Context *c)
{
  c->GPRx = 0;
}
static void sys_gettimeofday(Context *c)
{
  struct timeval *tv = (struct timeval *)c->GPR2;
  AM_TIMER_UPTIME_T uptime = io_read(AM_TIMER_UPTIME);
  tv->tv_sec = uptime.us / 1000000;
  tv->tv_usec = uptime.us % 1000000;
  c->GPRx = 0;
}
static void sys_execve(Context *c){
  printf("Sys execve\n");
  printf("GPR1: 0x%08x, GPR2: 0x%08x, GPR3: 0x%08x, GPR4: 0x%08x\n", c->GPR1, c->GPR2, c->GPR3, c->GPR4);
  const char *path = (const char *)c->GPR2;
  char *const *argv = (char *const *)c->GPR3;
  char *const *envp = (char *const *)c->GPR4;
  // check that the file exists before loading: if not, return -1 to the
  // user program (e.g. the shell) so it can keep running instead of the
  // kernel panicking in the loader
  int fd = fs_open(path, 0, 0);
  if (fd < 0) {
    printf("execve: file '%s' not found, return -1\n", path);
    c->GPRx = -1;
    return;
  }
  fs_close(fd);

  // 在 A (current) 的 PCB 内核栈上创建新程序 B 的上下文,
  // 并把 B 的 argc/argv/envp 放到 new_page() 新分配的用户栈上
  context_uload(current, path, argv, envp);
  // 将 current 指回 boot: 这样随后 yield() 的 schedule 保存的是 boot 的
  // 上下文, 而不会覆盖掉刚写好的 B 的上下文;
  // 此后 A 的执行流不会再被调度, 轮到 A 的 PCB 时执行的是 B
  switch_boot_pcb();
  yield();
  panic("should not reach here");
}
static void sys_exit(Context *c)
{
  naive_uload(NULL, "/bin/nterm");
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
  case SYS_open:
    sys_open(c);
    break;
  case SYS_read:
    sys_read(c);
    break;
  case SYS_write:
    sys_write(c);
    break;
  case SYS_close:
    sys_close(c);
    break;
  case SYS_lseek:
    sys_lseek(c);
    break;
  case SYS_brk:
    sys_brk(c);
    break;
  case SYS_gettimeofday:
    sys_gettimeofday(c);
    break;
  case SYS_execve:
    sys_execve(c);
    break;
  default:
    panic("Unhandled syscall ID = %d", a[0]);
  }
}
