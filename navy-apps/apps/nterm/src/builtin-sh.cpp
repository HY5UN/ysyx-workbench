#include <nterm.h>
#include <stdarg.h>
#include <unistd.h>
#include <string.h>
#include <SDL.h>

char handle_key(SDL_Event *ev);

static void sh_printf(const char *format, ...) {
  static char buf[256] = {};
  va_list ap;
  va_start(ap, format);
  int len = vsnprintf(buf, 256, format, ap);
  va_end(ap);
  term->write(buf, len);
}

static void sh_banner() {
  sh_printf("Built-in Shell in NTerm (NJU Terminal)\n\n");
}

static void sh_prompt() {
  sh_printf("sh> ");
}

static void sh_handle_cmd(const char *cmd) {
  /* copy the command into a local buffer, since we will split it in
   * place; the string handed to us is a buffer of the Terminal object,
   * with a trailing '\n' appended by keypress() */
  char line[256];
  strncpy(line, cmd, sizeof(line) - 1);
  line[sizeof(line) - 1] = '\0';

  /* parse the command line like the PA1 debugger: skip leading spaces,
   * the first word is the command name, the rest are its arguments */
  char *p = line;
  while (*p == ' ' || *p == '\t') p ++;

  if (strncmp(p, "echo", 4) == 0 &&
      (p[4] == ' ' || p[4] == '\t' || p[4] == '\n' || p[4] == '\0')) {
    /* built-in echo: print the arguments (everything after the name),
     * trimming the trailing newline/whitespace that keypress() appends */
    p += 4;
    while (*p == ' ' || *p == '\t') p ++;
    int len = strlen(p);
    while (len > 0 && (p[len - 1] == '\n' || p[len - 1] == ' ' || p[len - 1] == '\t')) len --;
    sh_printf("%.*s\n", len, p);
    return;
  }

  /* other commands: split the line into words (space/tab/newline are
   * separators) and pass them to the program as argv. PATH is set to
   * /bin in builtin_sh_run, so a bare name like "menu" is resolved by
   * execvp()'s PATH lookup; e.g. "pal --skip" runs /bin/pal with
   * argv[1] = "--skip" */
  char *argv[64];
  int argc = 0;
  while (*p && argc < (int)(sizeof(argv) / sizeof(argv[0])) - 1) {
    while (*p == ' ' || *p == '\t' || *p == '\n') p ++;
    if (*p == '\0') break;
    argv[argc ++] = p;
    while (*p && *p != ' ' && *p != '\t' && *p != '\n') p ++;
    if (*p != '\0') *p ++ = '\0';
  }
  argv[argc] = NULL;
  if (argc == 0) return;

  if (execvp(argv[0], argv) == -1) {
    /* execvp returns -1 when the program does not exist (or failed to
     * load); report it and let the shell keep running */
    sh_printf("sh: %s: command not found\n", argv[0]);
  }
}

void builtin_sh_run() {
  /* set the program search path for execvp(). the overwrite parameter is 0
   * so that an existing PATH (e.g. the native environment when running
   * NTerm on Navy native) is kept */
  setenv("PATH", "/bin", 0);

  sh_banner();
  sh_prompt();

  while (1) {
    SDL_Event ev;
    if (SDL_PollEvent(&ev)) {
      if (ev.type == SDL_KEYUP || ev.type == SDL_KEYDOWN) {
        const char *res = term->keypress(handle_key(&ev));
        if (res) {
          sh_handle_cmd(res);
          sh_prompt();
        }
      }
    }
    refresh_terminal();
  }
}
