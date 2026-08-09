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

uint32_t SDL_GetTicks() {
  SDL_UNIMPLEMENTED();
  return 0;
}

void SDL_Delay(uint32_t ms) {
  SDL_UNIMPLEMENTED();
}
