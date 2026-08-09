#include <NDL.h>
#include <SDL.h>
#include <stdio.h>
#include <string.h>

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
  char buf[64] = {};
  /* NOTE: NDL_PollEvent returns 1/0 (event present), NOT the byte count,
   * so locate the end of the event by its trailing '\n' instead. */
  if (NDL_PollEvent(buf, sizeof(buf) - 1) == 0) return 0;

  /* NDL event format: "kd <keyname>\n" or "ku <keyname>\n" */
  char *nl = strchr(buf, '\n');
  if (nl) *nl = '\0';

  // printf("[miniSDL] SDL_PollEvent: raw NDL event: \"%s\"\n", buf);

  ev->type = (buf[0] == 'k' && buf[1] == 'd') ? SDL_KEYDOWN : SDL_KEYUP;
  char *name = buf + 3;

  /* keyname[] is index-aligned with the SDLK_* enum (same order as AM_KEYS) */
  ev->key.keysym.sym = 0;
  for (int i = 1; i < (int)(sizeof(keyname) / sizeof(keyname[0])); i++) {
    if (strcmp(keyname[i], name) == 0) {
      ev->key.keysym.sym = i;
      break;
    }
  }

  // printf("[miniSDL] SDL_PollEvent: type=%s sym=%d (%s)\n",
  //     ev->type == SDL_KEYDOWN ? "KEYDOWN" : "KEYUP",
  //     ev->key.keysym.sym,
  //     ev->key.keysym.sym != 0 ? keyname[ev->key.keysym.sym] : "UNKNOWN");
  return 1;
}

int SDL_WaitEvent(SDL_Event *event) {
  // [DEBUG] heartbeat while waiting:
  // static int first = 1;
  // uint32_t last = 0;
  // if (first) {
  //   printf("[miniSDL] SDL_WaitEvent: waiting for events...\n");
  //   first = 0;
  // }
  while (SDL_PollEvent(event) == 0) {
    // uint32_t now = NDL_GetTicks();
    // if (now - last >= 2000) {
    //   printf("[miniSDL] SDL_WaitEvent: still polling, uptime=%u ms\n", now);
    //   last = now;
    // }
  }
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
