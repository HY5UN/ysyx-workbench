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
    // Log("Hello World from Nanos-lite with arg '%s' for the %dth time!", (const char *)arg, j);
    printf(".");
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
// 并按简化版 Process Initialization 约定把 argc/argv/envp 放到用户栈上,
// 将 argc 的地址写入 GPRx, 由 Navy 的 _start 将其设置到 sp 中
static void context_uload(PCB *pcb, const char *filename, char *const argv[], char *const envp[]) {
  uintptr_t entry = loader(pcb, filename);
  Area kstack = {.start = pcb->stack, .end = pcb->stack + STACK_SIZE};
  pcb->cp = ucontext(&pcb->as, kstack, (void *)entry);

  // 用户栈上的参数布局 (自高地址向低地址):
  //   字符串区域 (各字符串以 '\0' 结尾, 顺序任意, 中间的 Unspecified 间隔取 0)
  //   envp 指针数组 + NULL, argv 指针数组 + NULL, argc
  // GPRx 指向 argc 所在地址, 即用户进程的初始 sp
  int argc = 0;
  while (argv[argc] != NULL) argc ++;
  int envc = 0;
  while (envp[envc] != NULL) envc ++;

  size_t str_size = 0;
  for (int i = 0; i < argc; i ++) str_size += strlen(argv[i]) + 1;
  for (int i = 0; i < envc; i ++) str_size += strlen(envp[i]) + 1;

  uintptr_t sp = (uintptr_t)heap.end;   // 用户栈顶

  // 字符串区域
  sp -= str_size;
  sp &= ~(sizeof(uintptr_t) - 1);       // 对齐到 4 字节
  char *str_area = (char *)sp;

  // argc 与 argv/envp 指针数组
  sp -= (1 + argc + 1 + envc + 1) * sizeof(uintptr_t);
  sp &= ~(uintptr_t)15;                 // 初始 sp 16 字节对齐 (riscv psABI), 此处间隔即图中的 Unspecified
  uintptr_t *args = (uintptr_t *)sp;

  // 拷贝字符串, 并把各字符串的地址填入指针数组
  char *p = str_area;
  uintptr_t *argv_slot = args + 1;
  for (int i = 0; i < argc; i ++) {
    argv_slot[i] = (uintptr_t)p;
    strcpy(p, argv[i]);
    p += strlen(argv[i]) + 1;
  }
  argv_slot[argc] = 0;                  // argv 数组以 NULL 结尾

  uintptr_t *envp_slot = argv_slot + argc + 1;
  for (int i = 0; i < envc; i ++) {
    envp_slot[i] = (uintptr_t)p;
    strcpy(p, envp[i]);
    p += strlen(envp[i]) + 1;
  }
  envp_slot[envc] = 0;                  // envp 数组以 NULL 结尾

  args[0] = (uintptr_t)argc;            // 栈底 (最低地址) 存放 argc

  pcb->cp->GPRx = (uintptr_t)args;      // GPRx = argc 的地址
}

void init_proc() {
  Log("Initializing processes...");

  context_kload(&pcb[0], hello_fun, "A");

  // 给仙剑奇侠传传递 --skip 参数, 跳过片头商标动画以加速测试
  char *argv[] = {"/bin/pal", "--skip", NULL};
  char *envp[] = {NULL};
  context_uload(&pcb[1], "/bin/pal", argv, envp);
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
