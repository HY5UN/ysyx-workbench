#include <proc.h>

#define MAX_NR_PROC 4

static PCB pcb[MAX_NR_PROC] __attribute__((used)) = {};
static PCB pcb_boot = {};
PCB *current = NULL;

void switch_boot_pcb() {
  current = &pcb_boot;
}

// 约定 arg 为字符串指针, hello_fun() 按 "%s" 解析
void hello_fun(void *arg) {
  int j = 1;
  while (1) {
    Log("Hello World from Nanos-lite with arg '%s' for the %dth time!", (const char *)arg, j);
    j ++;
    yield();
  }
}

// 封装创建内核上下文的过程: 调用 kcontext() 创建上下文, 并把返回的指针记录到 PCB 的 cp 中
static void context_kload(PCB *pcb, void (*entry)(void *), void *arg) {
  Area kstack = {.start = pcb->stack, .end = pcb->stack + STACK_SIZE};
  pcb->cp = kcontext(kstack, entry, arg);
}

// 创建用户进程的上下文: 加载 ELF 文件, 在内核栈上创建上下文,
// 并把用户栈顶 (heap.end) 写入 GPRx, 由 Navy 的 _start 将其设置到 sp 中
static void context_uload(PCB *pcb, const char *filename) {
  uintptr_t entry = loader(pcb, filename);
  Area kstack = {.start = pcb->stack, .end = pcb->stack + STACK_SIZE};
  pcb->cp = ucontext(&pcb->as, kstack, (void *)entry);
  pcb->cp->GPRx = (uintptr_t)heap.end;  // 用户栈顶
}

void init_proc() {
  Log("Initializing processes...");

  context_kload(&pcb[0], hello_fun, "A");
  context_uload(&pcb[1], "/bin/pal");
  switch_boot_pcb();

  // naive_uload(NULL, "/bin/menu");
  // naive_uload(NULL, "/bin/nterm");
  // naive_uload(NULL, "/bin/pal");

}

// 简单的双进程轮转调度: 保存当前上下文, 切换到另一个进程
Context* schedule(Context *prev) {
  current->cp = prev;                                    // 保存当前进程的上下文
  current = (current == &pcb[0]) ? &pcb[1] : &pcb[0];    // 切换到下一个进程
  return current->cp;                                    // 返回新进程的上下文
}
