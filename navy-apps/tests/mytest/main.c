/* mytest: 所有自定义测试的统一入口 (navy-apps/tests/mytest)
 *
 * 每个测试组由一个独立的 .c 文件实现, 导出 void xxx_test(void);
 * 在下面的 tests[] 表中登记, 并把源文件加入 Makefile 的 SRCS 即可。
 */
#include <stdio.h>

extern void fixedpt_test(void);

typedef void (*test_fn)(void);

static const struct {
  const char *name;
  test_fn fn;
} tests[] = {
  { "fixedpt", fixedpt_test },
};

#define NR_TESTS (sizeof(tests) / sizeof(tests[0]))

int main() {
  int i;
  for (i = 0; i < (int)NR_TESTS; i++) {
    printf("===== test group: %s =====\n", tests[i].name);
    tests[i].fn();
  }
  printf("All test groups done.\n");
  return 0;
}
