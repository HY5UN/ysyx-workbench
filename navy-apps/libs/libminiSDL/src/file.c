#include <sdl-file.h>
#include <stdio.h>

// Remind you when an unimplemented API is called: print a warning and continue.
#define SDL_UNIMPLEMENTED() \
  do { \
    printf("[miniSDL] %s is not implemented yet (%s:%d)\n", __func__, __FILE__, __LINE__); \
  } while (0)

SDL_RWops* SDL_RWFromFile(const char *filename, const char *mode) {
  SDL_UNIMPLEMENTED();
  return NULL;
}

SDL_RWops* SDL_RWFromMem(void *mem, int size) {
  SDL_UNIMPLEMENTED();
  return NULL;
}
