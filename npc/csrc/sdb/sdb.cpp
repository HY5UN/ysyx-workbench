#include "include/common.h"
#include "include/mem.h"
#include "include/CPU.h"
#include "string.h"
#include <readline/readline.h>
#include <readline/history.h>
#include "include/config.h"

CPU *cpu = nullptr;


static char *rl_gets()
{
  static char *line_read = NULL;

  if (line_read)
  {
    free(line_read);
    line_read = NULL;
  }

  line_read = readline("(npc) ");

  if (line_read && *line_read)
  {
    add_history(line_read);
  }

  return line_read;
}

static int cmd_c(char *args)
{
  cpu->execute(-1);
  return 0;
}

static int cmd_q(char *args)
{
  return -1;
}

static int cmd_si(char *args)
{
  char *steps_str = strtok(NULL, " ");
  int steps = 0;
  if (steps_str == NULL)
  {
    steps = 1;
  }
  else
  {
    steps = atoi(steps_str);
  }
  cpu->execute(steps);
  return 0;
}

static int cmd_info(char *args)
{
  char *subcmd = strtok(NULL, " ");
  if (subcmd == NULL)
  {
    printf("Please provide a subcommand: r for registers, w for watchpoints\n");
    return 0;
  }
  if (strcmp(subcmd, "r") == 0)
  {
    cpu->reg_print();
  }
  else if (strcmp(subcmd, "w") == 0)
  {
    printf("Not implemented yet.\n");
    //wp_display();
  }
  else
  {
    printf("Unknown subcommand '%s'\n", subcmd);
  }
  return 0;
}

static int cmd_x(char *args)
{
  char *n_str = strtok(NULL, " ");
  char *expr_str = strtok(NULL, " ");
  if (n_str == NULL || expr_str == NULL)
  {
    printf("Usage: x N EXPR\n");
    return 0;
  }
  int n = atoi(n_str);

  int vaddr_bits = sizeof(vaddr_t) * 8;
  if (strlen(expr_str) > vaddr_bits / 4 + 2)
  {
    printf("Expression too long: %s\n", expr_str);
    return 0;
  }
  char *end;
  errno = 0;
  vaddr_t expr = strtoul(expr_str, &end, 0);
  if (errno != 0 || *end != '\0')
  {
    printf("Invalid expression: %s\n", expr_str);
    return 0;
  }

  mem_print(expr, n * 4);
  return 0;
}

// static int cmd_p(char *args)
// {
//   if (args == NULL)
//   {
//     printf("Usage: p EXPR\n");
//     return 0;
//   }
//   bool success = true;

//   // 测试
//    test_expr(&success);

//   word_t result = expr(args, &success);
//   if (success)
//   {
//     printf(FMT_WORD "\n", result);
//   }
//   else
//   {
//     printf("Failed to evaluate expression: %s\n", args);
//   }
//   return 0;
// }

// ./watchpoint.c
// extern int cmd_w(char *args);
// extern int cmd_d(char *args);


static struct
{
  const char *name;
  const char *description;
  int (*handler)(char *);
} cmd_table[] = {
    {"c", "Continue the execution of the program", cmd_c},
    {"q", "Exit Simulation", cmd_q},
    {"si", "Step N instructions exactly", cmd_si},
    {"info", "Print program status", cmd_info},
     {"x", "Scan Memory", cmd_x},
    // {"p", "Evaluate expression", cmd_p},
    // {"w", "Watchpoint operations", cmd_w},
    // {"d", "Delete watchpoint", cmd_d}

};

#define NR_CMD (sizeof(cmd_table) / sizeof(cmd_table[0]))



void sdb_mainloop(int argc, char **argv)
{

  cpu = new CPU(argc, argv);
  cpu->reset(10);


  #ifdef BATCH_MODE
    cmd_c(NULL);
    return;
  #endif

  for (char *str; (str = rl_gets()) != NULL;)
  {
    char *str_end = str + strlen(str);

    /* extract the first token as the command */
    char *cmd = strtok(str, " ");
    if (cmd == NULL)
    {
      continue;
    }

    /* treat the remaining string as the arguments,
     * which may need further parsing
     */
    char *args = cmd + strlen(cmd) + 1;
    if (args >= str_end)
    {
      args = NULL;
    }

    int i;
    for (i = 0; i < NR_CMD; i++)
    {
      if (strcmp(cmd, cmd_table[i].name) == 0)
      {
        if (cmd_table[i].handler(args) < 0)
        {
          return;
        }
        break;
      }
    }

    if (i == NR_CMD)
    {
      printf("Unknown command '%s'\n", cmd);
    }
  }
}