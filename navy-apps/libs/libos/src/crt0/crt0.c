#include <stdint.h>
#include <stdlib.h>
#include <assert.h>

int main(int argc, char *argv[], char *envp[]);
extern char **environ;
// 用户栈上的参数布局由 Nanos-lite 按简化版 Process Initialization 约定构造:
//   args[0] = argc, args[1..argc] = argv[0..argc-1], args[argc+1] = NULL,
//   其后依次是 envp 指针数组与 NULL, 再往上是字符串区域
void call_main(uintptr_t *args) {
  int argc = (int)args[0];
  char **argv = (char **)(args + 1);
  char **envp = argv + argc + 1;
  environ = envp;
  exit(main(argc, argv, envp));
  assert(0);
}
