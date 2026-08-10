#include <NDL.h>
#include <sdl-timer.h>
#include <stdio.h>

// Remind you when an unimplemented API is called: print a warning and continue.
#define SDL_UNIMPLEMENTED() \
  do { \
    printf("[miniSDL] %s is not implemented yet (%s:%d)\n", __func__, __FILE__, __LINE__); \
  } while (0)

SDL_TimerID SDL_AddTimer(uint32_t interval, SDL_NewTimerCallback callback, void *param) {
  SDL_UNIMPLEMENTED();
  return NULL;
}

int SDL_RemoveTimer(SDL_TimerID id) {
  SDL_UNIMPLEMENTED();
  return 1;
}

static uint32_t sdl_start_ticks = 0;

/* called once by SDL_Init: record the boot-relative tick count, so that
 * SDL_GetTicks() counts milliseconds since SDL initialization (per the SDL spec) */
void SDL_StartTicks() {
  sdl_start_ticks = NDL_GetTicks();
}

uint32_t SDL_GetTicks() {
  return NDL_GetTicks() - sdl_start_ticks;
}

void SDL_Delay(uint32_t ms) {
  /* busy-wait on the SDL-relative clock; unsigned subtraction is wrap-safe */
  uint32_t start = SDL_GetTicks();
  while (SDL_GetTicks() - start < ms);
}
