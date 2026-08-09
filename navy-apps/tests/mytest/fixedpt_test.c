/* fixedptc 定点算术测试
 *
 * 环境约定: Navy 中无浮点运行时 (newlib 编译时定义 NO_FLOATING_POINT),
 * 因此本测试只用整数运算, 所有期望值均以 fixedpt 整数 (真值 x2^8) 手工计算。
 *
 * fixedpt 格式: 24.8, FIXEDPT_ONE = 256 = 1.0
 */
#include <stdio.h>
#include <fixedptc.h>

static int n_pass = 0;
static int n_fail = 0;

#define CHECK(cond, msg, ...) do { \
  if (cond) { n_pass++; } \
  else { n_fail++; printf("FAIL %s:%d: " msg "\n", __FILE__, __LINE__, ##__VA_ARGS__); } \
} while (0)

/* 转换与常量: rconst/fromint/toint/fracpart */
static void test_convert(void) {
  /* 1.2 * 2^8 = 307.2 -> 307 = 0x133 (讲义中的例子) */
  CHECK(fixedpt_rconst(1.2) == 307, "rconst(1.2) == 0x133(307), got %d", fixedpt_rconst(1.2));
  CHECK(fixedpt_rconst(-1.2) == -307, "rconst(-1.2) == -307, got %d", fixedpt_rconst(-1.2));
  CHECK(fixedpt_rconst(1.0) == FIXEDPT_ONE, "rconst(1.0) == FIXEDPT_ONE");
  CHECK(fixedpt_fracpart(fixedpt_rconst(1.2)) == 0x33, "fracpart(1.2) == 0x33, got %d", fixedpt_fracpart(fixedpt_rconst(1.2)));

  CHECK(FIXEDPT_ONE == 256, "FIXEDPT_ONE == 256");
  CHECK(fixedpt_fromint(3) == 768, "fromint(3) == 768, got %d", fixedpt_fromint(3));
  CHECK(fixedpt_fromint(-3) == -768, "fromint(-3) == -768, got %d", fixedpt_fromint(-3));
  CHECK(fixedpt_toint(fixedpt_fromint(3)) == 3, "toint(fromint(3)) == 3, got %d", fixedpt_toint(fixedpt_fromint(3)));
  CHECK(fixedpt_toint(fixedpt_fromint(-3)) == -3, "toint(fromint(-3)) == -3, got %d", fixedpt_toint(fixedpt_fromint(-3)));
}

/* 加减法: 直接使用整数 + - (补码性质) */
static void test_add_sub(void) {
  fixedpt a = fixedpt_rconst(1.2); /* 307 */
  fixedpt b = fixedpt_rconst(2.3); /* 589 */

  CHECK(a + b == 896, "1.2 + 2.3 == 3.5, got %d", a + b);
  CHECK(b - a == 282, "2.3 - 1.2 == 282/256, got %d", b - a);
  CHECK(fixedpt_add(a, b) == 896, "fixedpt_add(1.2, 2.3) == 3.5");
  CHECK(fixedpt_sub(b, a) == 282, "fixedpt_sub(2.3, 1.2) == 282/256");
}

/* 与整数的乘除 */
static void test_mul_divi(void) {
  /* 1.5 * 4 = 6.0 (精确) */
  CHECK(fixedpt_muli(fixedpt_rconst(1.5), 4) == fixedpt_fromint(6),
        "muli(1.5, 4) == 6, got %d", fixedpt_muli(fixedpt_rconst(1.5), 4));
  /* -1.5 * 4 = -6.0 (精确) */
  CHECK(fixedpt_muli(fixedpt_rconst(-1.5), 4) == fixedpt_fromint(-6),
        "muli(-1.5, 4) == -6, got %d", fixedpt_muli(fixedpt_rconst(-1.5), 4));
  /* 1.5 * -4 = -6.0 (精确) */
  CHECK(fixedpt_muli(fixedpt_rconst(1.5), -4) == fixedpt_fromint(-6),
        "muli(1.5, -4) == -6, got %d", fixedpt_muli(fixedpt_rconst(1.5), -4));
  /* 10 / 4 = 2.5 (精确) */
  CHECK(fixedpt_divi(fixedpt_fromint(10), 4) == 640,
        "divi(10, 4) == 2.5, got %d", fixedpt_divi(fixedpt_fromint(10), 4));
  /* -10 / 4 = -2.5 (精确) */
  CHECK(fixedpt_divi(fixedpt_fromint(-10), 4) == -640,
        "divi(-10, 4) == -2.5, got %d", fixedpt_divi(fixedpt_fromint(-10), 4));
}

/* fixedpt 之间的乘除: 结果需要调整 2^8 */
static void test_mul_div(void) {
  /* 1.5 * 2.0 = 3.0 (精确, 无舍入) */
  CHECK(fixedpt_mul(fixedpt_rconst(1.5), fixedpt_rconst(2.0)) == fixedpt_fromint(3),
        "mul(1.5, 2.0) == 3, got %d", fixedpt_mul(fixedpt_rconst(1.5), fixedpt_rconst(2.0)));
  /* -1.5 * 2.0 = -3.0 (精确) */
  CHECK(fixedpt_mul(fixedpt_rconst(-1.5), fixedpt_rconst(2.0)) == fixedpt_fromint(-3),
        "mul(-1.5, 2.0) == -3, got %d", fixedpt_mul(fixedpt_rconst(-1.5), fixedpt_rconst(2.0)));
  /* 1.2 * 2.3 = 2.76 -> 307*589>>8 = 706 (706/256 = 2.7578125) */
  CHECK(fixedpt_mul(fixedpt_rconst(1.2), fixedpt_rconst(2.3)) == 706,
        "mul(1.2, 2.3) == 706/256, got %d", fixedpt_mul(fixedpt_rconst(1.2), fixedpt_rconst(2.3)));

  /* 10 / 4 = 2.5 (精确) */
  CHECK(fixedpt_div(fixedpt_fromint(10), fixedpt_fromint(4)) == 640,
        "div(10, 4) == 2.5, got %d", fixedpt_div(fixedpt_fromint(10), fixedpt_fromint(4)));
  /* -10 / 4 = -2.5 (精确) */
  CHECK(fixedpt_div(fixedpt_fromint(-10), fixedpt_fromint(4)) == -640,
        "div(-10, 4) == -2.5, got %d", fixedpt_div(fixedpt_fromint(-10), fixedpt_fromint(4)));
  /* 1 / 2 = 0.5 (精确) */
  CHECK(fixedpt_div(FIXEDPT_ONE, FIXEDPT_TWO) == 128,
        "div(1, 2) == 0.5, got %d", fixedpt_div(FIXEDPT_ONE, FIXEDPT_TWO));
}

/* 绝对值 */
static void test_abs(void) {
  CHECK(fixedpt_abs(fixedpt_fromint(7)) == fixedpt_fromint(7), "abs(7) == 7");
  CHECK(fixedpt_abs(fixedpt_fromint(-7)) == fixedpt_fromint(7), "abs(-7) == 7, got %d", fixedpt_abs(fixedpt_fromint(-7)));
  CHECK(fixedpt_abs(0) == 0, "abs(0) == 0");
}

/* floor/ceil: 必须严格符合 man floor()/ceil() 的语义
 *   floor(x) = 不大于 x 的最大整数;  ceil(x) = 不小于 x 的最小整数
 * 特别注意负数: floor(-1.2) = -2, ceil(-1.2) = -1
 */
static void test_floor_ceil(void) {
  CHECK(fixedpt_floor(fixedpt_rconst(1.2)) == fixedpt_fromint(1), "floor(1.2) == 1, got %d", fixedpt_floor(fixedpt_rconst(1.2)));
  CHECK(fixedpt_floor(fixedpt_rconst(1.0)) == fixedpt_fromint(1), "floor(1.0) == 1");
  CHECK(fixedpt_floor(fixedpt_rconst(0.2)) == fixedpt_fromint(0), "floor(0.2) == 0, got %d", fixedpt_floor(fixedpt_rconst(0.2)));
  CHECK(fixedpt_floor(fixedpt_rconst(-0.2)) == fixedpt_fromint(-1), "floor(-0.2) == -1, got %d", fixedpt_floor(fixedpt_rconst(-0.2)));
  CHECK(fixedpt_floor(fixedpt_rconst(-1.0)) == fixedpt_fromint(-1), "floor(-1.0) == -1");
  CHECK(fixedpt_floor(fixedpt_rconst(-1.2)) == fixedpt_fromint(-2), "floor(-1.2) == -2, got %d", fixedpt_floor(fixedpt_rconst(-1.2)));

  CHECK(fixedpt_ceil(fixedpt_rconst(1.2)) == fixedpt_fromint(2), "ceil(1.2) == 2, got %d", fixedpt_ceil(fixedpt_rconst(1.2)));
  CHECK(fixedpt_ceil(fixedpt_rconst(1.0)) == fixedpt_fromint(1), "ceil(1.0) == 1");
  CHECK(fixedpt_ceil(fixedpt_rconst(0.2)) == fixedpt_fromint(1), "ceil(0.2) == 1, got %d", fixedpt_ceil(fixedpt_rconst(0.2)));
  CHECK(fixedpt_ceil(fixedpt_rconst(-0.2)) == fixedpt_fromint(0), "ceil(-0.2) == 0, got %d", fixedpt_ceil(fixedpt_rconst(-0.2)));
  CHECK(fixedpt_ceil(fixedpt_rconst(-1.0)) == fixedpt_fromint(-1), "ceil(-1.0) == -1");
  CHECK(fixedpt_ceil(fixedpt_rconst(-1.2)) == fixedpt_fromint(-1), "ceil(-1.2) == -1, got %d", fixedpt_ceil(fixedpt_rconst(-1.2)));
}

/* 讲义中的示例: c = (a + 1) * b / 2.3, 其中 a = 1.2, b = 10, c 为整数
 * 实数结果为 (1.2+1)*10/2.3 = 9.5652..., 取整后 c = 9 */
static void test_lecture_example(void) {
  fixedpt a = fixedpt_rconst(1.2);
  fixedpt b = fixedpt_fromint(10);
  int c = fixedpt_toint(fixedpt_div(fixedpt_mul(a + FIXEDPT_ONE, b), fixedpt_rconst(2.3)));
  CHECK(c == 9, "(1.2+1)*10/2.3 == 9, got %d", c);
}

/* 库自带函数 (sqrt/sin/exp/ln) 的冒烟测试, 精度损失大, 用范围断言 */
static void test_lib_bonus(void) {
  int s;

  s = fixedpt_sqrt(fixedpt_fromint(9));
  CHECK(s >= fixedpt_fromint(3) - 2 && s <= fixedpt_fromint(3) + 2,
        "sqrt(9) ~= 3, got %d", s);
  CHECK(fixedpt_sin(0) == 0, "sin(0) == 0, got %d", fixedpt_sin(0));
  s = fixedpt_sin(FIXEDPT_HALF_PI);
  CHECK(s >= FIXEDPT_ONE - 20 && s <= FIXEDPT_ONE + 20,
        "sin(pi/2) ~= 1, got %d", s);
  CHECK(fixedpt_exp(0) == FIXEDPT_ONE, "exp(0) == 1, got %d", fixedpt_exp(0));
  CHECK(fixedpt_ln(FIXEDPT_ONE) == 0, "ln(1) == 0, got %d", fixedpt_ln(FIXEDPT_ONE));
  s = fixedpt_ln(FIXEDPT_E);
  CHECK(s >= FIXEDPT_ONE - 10 && s <= FIXEDPT_ONE + 10,
        "ln(e) ~= 1, got %d", s);
}

void fixedpt_test(void) {
  test_convert();
  test_add_sub();
  test_mul_divi();
  test_mul_div();
  test_abs();
  test_floor_ceil();
  test_lecture_example();
  test_lib_bonus();
  printf("fixedpt: %d passed, %d failed\n", n_pass, n_fail);
}
