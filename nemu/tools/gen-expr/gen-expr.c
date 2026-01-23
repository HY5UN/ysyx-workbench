#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
#include <assert.h>
#include <string.h>

// 缓冲区大小
#define BUF_SIZE 32

static char buf[BUF_SIZE] = {};
static char code_buf[BUF_SIZE + 128] = {};
static char *code_format =
"#include <stdio.h>\n"
"int main() { "
"  unsigned result = %s; "
"  printf(\"%%u\", result); "
"  return 0; "
"}";

// 全局变量：记录 buf 当前写入的位置
static int pos = 0;

// 辅助函数：生成 [0, n-1] 的随机数
static inline uint32_t choose(uint32_t n) {
  return rand() % n;
}

// 辅助函数：向 buf 写入一个字符
static inline void gen(char c) {
  if (pos < BUF_SIZE - 1) {
    buf[pos++] = c;
  }
}

// 辅助函数：生成一个随机数
static inline void gen_num() {
  // 生成 100 以内的数，保持表达式简短可读
  // 避免生成0以减少除零错误的概率，但不是完全避免
  pos += sprintf(buf + pos, "%u", rand() % 100); 
}

// 辅助函数：生成一个随机运算符
static inline void gen_rand_op() {
  switch (choose(4)) {
    case 0: gen('+'); break;
    case 1: gen('-'); break;
    case 2: gen('*'); break;
    case 3: gen('/'); break;
  }
}

static void gen_rand_expr() {
  // 核心保护机制：如果 buffer 快满了，强制生成数字以结束递归
  // 防止 gen_rand_expr 无限调用导致栈溢出或 buffer 溢出
  if (pos > 20) {
    gen_num();
    return;
  }

  switch (choose(3)) {
    case 0: 
      gen_num(); 
      break;
    case 1: 
      gen('('); 
      gen_rand_expr(); 
      gen(')'); 
      break;
    default: 
      gen_rand_expr(); 
      gen_rand_op(); 
      gen_rand_expr(); 
      break;
  }
}

int main(int argc, char *argv[]) {
  int seed = time(0);
  srand(seed);
  int loop = 1;
  if (argc > 1) {
    sscanf(argv[1], "%d", &loop);
  }
  int i;
  for (i = 0; i < loop; i ++) {
    // 【重要】每次生成前重置 buffer 位置
    pos = 0;
    buf[0] = '\0';

    gen_rand_expr();
    
    // 生成结束后手动添加字符串结束符
    buf[pos] = '\0';

    sprintf(code_buf, code_format, buf);

    FILE *fp = fopen("/tmp/.code.c", "w");
    assert(fp != NULL);
    fputs(code_buf, fp);
    fclose(fp);

    int ret = system("gcc /tmp/.code.c -o /tmp/.expr");
    if (ret != 0) continue;

    fp = popen("/tmp/.expr", "r");
    assert(fp != NULL);

    int result;
    ret = fscanf(fp, "%d", &result);
    pclose(fp);

    printf("%u %s\n", result, buf);
  }
  return 0;
}