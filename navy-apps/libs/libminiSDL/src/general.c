#include <NDL.h>
#include <stdio.h>

// Remind you when an unimplemented API is called: print a warning and continue.
#define SDL_UNIMPLEMENTED() \
  do { \
    printf("[miniSDL] %s is not implemented yet (%s:%d)\n", __func__, __FILE__, __LINE__); \
  } while (0)

void SDL_StartTicks();  // defined in timer.c, called below

int SDL_Init(uint32_t flags) {
  int r = NDL_Init(flags);
  SDL_StartTicks();
  return r;
}

void SDL_Quit() {
  NDL_Quit();
}

char *SDL_GetError() {
  return "Navy does not support SDL_GetError()";
}

int SDL_SetError(const char* fmt, ...) {
  SDL_UNIMPLEMENTED();
  return -1;
}

int SDL_ShowCursor(int toggle) {
  SDL_UNIMPLEMENTED();
  return 0;
}

void SDL_WM_SetCaption(const char *title, const char *icon) {
  SDL_UNIMPLEMENTED();
}
