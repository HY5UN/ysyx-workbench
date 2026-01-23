/***************************************************************************************
 * Copyright (c) 2014-2024 Zihao Yu, Nanjing University
 *
 * NEMU is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 ***************************************************************************************/

#include <isa.h>

/* We use the POSIX regex functions to process regular expressions.
 * Type 'man regex' for more information about POSIX regex functions.
 */
#include <regex.h>

enum
{
  TK_NOTYPE = 256,
  TK_EQ,

  /* TODO: Add more token types */
  TK_NUM

};

static struct rule
{
  const char *regex;
  int token_type;
} rules[] = {

    /* TODO: Add more rules.
     * Pay attention to the precedence level of different rules.
     */

    {" +", TK_NOTYPE},  // spaces
    {"\\+", '+'},       // plus
    {"==", TK_EQ},      // equal
    {"[0-9]+", TK_NUM}, // number
    {"\\-", '-'},       // minus
    {"\\*", '*'},       // multiply
    {"/", '/'},         // divide
    {"\\(", '('},       // left parenthesis
    {"\\)", ')'},       // right parenthesis

};

#define NR_REGEX ARRLEN(rules)

static regex_t re[NR_REGEX] = {};

/* Rules are used for many times.
 * Therefore we compile them only once before any usage.
 */
void init_regex()
{
  int i;
  char error_msg[128];
  int ret;

  for (i = 0; i < NR_REGEX; i++)
  {
    ret = regcomp(&re[i], rules[i].regex, REG_EXTENDED);
    if (ret != 0)
    {
      regerror(ret, &re[i], error_msg, 128);
      panic("regex compilation failed: %s\n%s", error_msg, rules[i].regex);
    }
  }
}

typedef struct token
{
  int type;
  char str[32];
} Token;

static Token tokens[32] __attribute__((used)) = {};
static int nr_token __attribute__((used)) = 0;

static bool make_token(char *e)
{
  int position = 0;
  int i;
  regmatch_t pmatch;

  nr_token = 0;

  while (e[position] != '\0')
  {
    /* Try all rules one by one. */
    for (i = 0; i < NR_REGEX; i++)
    {
      if (regexec(&re[i], e + position, 1, &pmatch, 0) == 0 && pmatch.rm_so == 0)
      {
        char *substr_start = e + position;
        int substr_len = pmatch.rm_eo;

        Log("match rules[%d] = \"%s\" at position %d with len %d: %.*s",
            i, rules[i].regex, position, substr_len, substr_len, substr_start);

        position += substr_len;

        /* TODO: Now a new token is recognized with rules[i]. Add codes
         * to record the token in the array `tokens'. For certain types
         * of tokens, some extra actions should be performed.
         */
        if (rules[i].token_type != TK_NOTYPE)
        {
          tokens[nr_token].type = rules[i].token_type;
          if (rules[i].token_type == TK_NUM)
          {
            Assert(substr_len < sizeof(tokens[nr_token].str), "number too long");
            strncpy(tokens[nr_token].str, substr_start, substr_len);
          }
          Assert(nr_token < ARRLEN(tokens), "too many tokens");
          nr_token++;
        }

        switch (rules[i].token_type)
        {
        default: // TODO();
          break;
        }

        break;
      }
    }

    if (i == NR_REGEX)
    {
      printf("no match at position %d\n%s\n%*.s^\n", position, e, position, "");
      return false;
    }
  }

  return true;
}

static bool check_parentheses(int p, int q)
{
  if (tokens[p].type != '(' || tokens[q].type != ')')
  {
    return false;
  }

  int cnt = 0;
  for (int i = p; i <= q; i++)
  {
    if (tokens[i].type == '(')
      cnt++;
    else if (tokens[i].type == ')')
      cnt--;
    if (cnt == 0 && i < q)
      return false;
  }
  return cnt == 0;
}


word_t expr(char *e, bool *success)
{
  if (!make_token(e))
  {
    *success = false;
    return 0;
  }

  //test
  printf("开始从 input 文件加载测试用例...\n");

    // 打开 nemu 目录下的 input 文件
    FILE *fp = fopen("input", "r");
    if (fp == NULL) {
        printf("错误：无法打开 input 文件。请确保该文件位于 nemu 目录下。\n");
        return 0;
    }

    char buf[65536];
    uint32_t ref_val;
    uint32_t count = 0;

    // 循环读取每一行：先读结果，再读表达式
    // 格式字符串 "%u %[^\n]" 的含义：
    // %u: 读取一个无符号整数 (结果)
    // [空格]: 跳过结果和表达式之间的空格
    // %[^\n]: 读取字符串直到遇到换行符 (表达式本体)
    while (fscanf(fp, "%u %[^\n]", &ref_val, buf) == 2) {
        bool success = false;
        
        // 调用你实现的 expr 函数
        uint32_t my_val = expr(buf, &success);

        // 验证计算是否成功
        if (!success) {
            printf("测试失败：expr 返回 false\n");
            printf("表达式: %s\n", buf);
            assert(0);
        }

        // 验证结果是否一致
        if (my_val != ref_val) {
            printf("测试失败：结果不匹配！\n");
            printf("表达式:   %s\n", buf);
            printf("预期结果: %u\n", ref_val);
            printf("实际结果: %u\n", my_val);
            assert(0); // 触发断言暂停程序
        }
        
        count++;
        // 可选：每通过 1000 个测试打印一次进度
        if (count % 1000 == 0) {
            printf("已通过 %u 个测试用例\n", count);
        }
    }

    printf("恭喜！所有 %u 个测试用例全部通过！\n", count);
    fclose(fp);

  /* TODO: Insert codes to evaluate the expression. */

  // TODO();

  return 0;
}

word_t eval(int p, int q, bool *success)
{
  if (p > q)
  {
    *success = false;
    return 0;
  }
  else if (p == q)
  {
    return atoi(tokens[p].str);
  }
  else if (check_parentheses(p, q))
  {
    return eval(p + 1, q - 1, success);
  }
  else
  {
    int op = -1;
    bool in_paren = false;
    for (int i = p; i <= q; i++)
    {
      if (tokens[i].type == '(')
        in_paren = true;
      else if (tokens[i].type == ')')
        in_paren = false;
      else if (!in_paren)
      {
        if (tokens[i].type == '+' || tokens[i].type == '-')
        {
          op = i;
        }
        else if (op == -1 && (tokens[i].type == '*' || tokens[i].type == '/'))
        {
          op = i;
        }
      }
    }
    if (op == -1)
    {
      *success = false;
      return 0;
    }

    word_t val1 = eval(p, op - 1, success);
    word_t val2 = eval(op + 1, q, success);
    if (!*success)
      return 0;
    char op_type = tokens[op].type;
    switch (op_type)
    {
    case '+':
      return val1 + val2;
    case '-':
      return val1 - val2;
    case '*':
      return val1 * val2;
    case '/':
      if (val2 == 0)
      {
        *success = false;
        return 0;
      }
      return val1 / val2;
    default:
      *success = false;
      return 0;
    }
  }

  return 0;
}

