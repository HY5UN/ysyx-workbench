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
#include <cpu/cpu.h>
#include <readline/readline.h>
#include <readline/history.h>
#include<errno.h>
#include <limits.h>
#include <memory/vaddr.h>
#include "sdb.h"

static int is_batch_mode = false;

void init_regex();
void init_wp_pool();

/* We use the `readline' library to provide more flexibility to read from stdin. */
static char* rl_gets() {
  static char *line_read = NULL;

  if (line_read) {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(nemu) ");

  if (line_read && *line_read) {
    add_history(line_read);
  }

  return line_read;
}

static int cmd_c(char *args) {
  cpu_exec(-1);
  return 0;
}


static int cmd_q(char *args) {
  return -1;
}

static int cmd_si(char *args){
  char *steps_str = strtok(NULL, " ");
  int steps=0;
  if(steps_str == NULL){
    steps = 1;
  }else{
    steps = atoi(steps_str);
  }
  cpu_exec(steps);
  return 0;
}

static int cmd_info(char *args){
  char *subcmd = strtok(NULL, " ");
  if(subcmd == NULL){
    printf("Please provide a subcommand: r for registers, w for watchpoints\n");
    return 0;
  }
  if(strcmp(subcmd,"r")==0){
    isa_reg_display();
  }else if(strcmp(subcmd,"w")==0){
    //待实现
  }
  else{
    printf("Unknown subcommand '%s'\n", subcmd );
  }
  return 0;
}

static int cmd_x(char *args){
  char *n_str = strtok(NULL, " ");
  char *expr_str = strtok(NULL, " ");
  if(n_str == NULL || expr_str == NULL){
    printf("Usage: x N EXPR\n");
    return 0;
  }
  int n = atoi(n_str);

  int vaddr_bits = sizeof(vaddr_t) * CHAR_BIT;
  if(strlen(expr_str)>vaddr_bits/4+2){
    printf("Expression too long: %s\n", expr_str);
    return 0;
  }
  char *end;
  errno = 0;
  vaddr_t expr = strtoul(expr_str, &end, 0);
  if (errno != 0 || *end != '\0'){
    printf("Invalid expression: %s\n", expr_str);
    return 0;
  }

  for(int i=0;i<n;i++){
    vaddr_t addr = expr + i * 4;
    word_t data = vaddr_read(addr,4);
    printf(FMT_WORD": "FMT_WORD"\n", addr, data);
  }
  
  return 0;

}

static int cmd_p(char *args){
  if(args == NULL){
    printf("Usage: p EXPR\n");
    return 0;
  }
  bool success = true;
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
        //bool success = false;
        
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
  word_t result = expr(args, &success);
  if(success){
    printf(FMT_WORD"\n", result);
  }else{
    printf("Failed to evaluate expression: %s\n", args);
  }
  return 0;
}

static int cmd_help(char *args);

static struct {
  const char *name;
  const char *description;
  int (*handler) (char *);
} cmd_table [] = {
  { "help", "Display information about all supported commands", cmd_help },
  { "c", "Continue the execution of the program", cmd_c },
  { "q", "Exit NEMU", cmd_q },

  /* TODO: Add more commands */
  {"si","Step N instructions exactly",cmd_si},
  {"info","Print program status",cmd_info},
  {"x","Scan Memory",cmd_x},
  {"p","Evaluate expression",cmd_p}


};

#define NR_CMD ARRLEN(cmd_table)

static int cmd_help(char *args) {
  /* extract the first argument */
  char *arg = strtok(NULL, " ");
  int i;

  if (arg == NULL) {
    /* no argument given */
    for (i = 0; i < NR_CMD; i ++) {
      printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
    }
  }
  else {
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(arg, cmd_table[i].name) == 0) {
        printf("%s - %s\n", cmd_table[i].name, cmd_table[i].description);
        return 0;
      }
    }
    printf("Unknown command '%s'\n", arg);
  }
  return 0;
}

void sdb_set_batch_mode() {
  is_batch_mode = true;
}

void sdb_mainloop() {
  if (is_batch_mode) {
    cmd_c(NULL);
    return;
  }

  for (char *str; (str = rl_gets()) != NULL; ) {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL) { continue; }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end) {
      args = NULL;
    }

#ifdef CONFIG_DEVICE
    extern void sdl_clear_event_queue();
    sdl_clear_event_queue();
#endif

    int i;
    for (i = 0; i < NR_CMD; i ++) {
      if (strcmp(cmd, cmd_table[i].name) == 0) {
        if (cmd_table[i].handler(args) < 0) { 
          nemu_state.state = NEMU_QUIT;
          return; }
        break;
      }
    }

    if (i == NR_CMD) { printf("Unknown command '%s'\n", cmd); }
  }
}

void init_sdb() {
  /* Compile the regular expressions. */
  init_regex();

  /* Initialize the watchpoint pool. */
  init_wp_pool();
}
