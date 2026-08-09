#include <NDL.h>
#include <SDL.h>
#include <stdio.h>

// Remind you when an unimplemented API is called: print a warning and continue.
#define SDL_UNIMPLEMENTED() \
  do { \
    printf("[miniSDL] %s is not implemented yet (%s:%d)\n", __func__, __FILE__, __LINE__); \
  } while (0)

#define keyname(k) #k,

static const char *keyname[] = {
  "NONE",
  _KEYS(keyname)
};

int SDL_PushEvent(SDL_Event *ev) {
  SDL_UNIMPLEMENTED();
  return 0;
}

int SDL_PollEvent(SDL_Event *ev) {
  SDL_UNIMPLEMENTED();
  return 0;
}

int SDL_WaitEvent(SDL_Event *event) {
  SDL_UNIMPLEMENTED();
  return 1;
}

int SDL_PeepEvents(SDL_Event *ev, int numevents, int action, uint32_t mask) {
  SDL_UNIMPLEMENTED();
  return 0;
}

uint8_t* SDL_GetKeyState(int *numkeys) {
  SDL_UNIMPLEMENTED();
  return NULL;
}
