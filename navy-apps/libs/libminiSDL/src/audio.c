#include <NDL.h>
#include <SDL.h>
#include <stdio.h>

// Remind you when an unimplemented API is called: print a warning and continue.
#define SDL_UNIMPLEMENTED() \
  do { \
    printf("[miniSDL] %s is not implemented yet (%s:%d)\n", __func__, __FILE__, __LINE__); \
  } while (0)

int SDL_OpenAudio(SDL_AudioSpec *desired, SDL_AudioSpec *obtained) {
  SDL_UNIMPLEMENTED();
  return 0;
}

void SDL_CloseAudio() {
  SDL_UNIMPLEMENTED();
}

void SDL_PauseAudio(int pause_on) {
  SDL_UNIMPLEMENTED();
}

void SDL_MixAudio(uint8_t *dst, uint8_t *src, uint32_t len, int volume) {
  SDL_UNIMPLEMENTED();
}

SDL_AudioSpec *SDL_LoadWAV(const char *file, SDL_AudioSpec *spec, uint8_t **audio_buf, uint32_t *audio_len) {
  SDL_UNIMPLEMENTED();
  return NULL;
}

void SDL_FreeWAV(uint8_t *audio_buf) {
  SDL_UNIMPLEMENTED();
}

void SDL_LockAudio() {
}

void SDL_UnlockAudio() {
}
