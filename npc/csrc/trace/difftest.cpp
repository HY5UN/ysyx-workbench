#include"include/common.h"
#include"include/trace.h"
typedef struct {
  word_t gpr[32];
  vaddr_t pc;
} riscv32_CPU_state;