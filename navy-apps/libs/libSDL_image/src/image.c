#define SDL_malloc  malloc
#define SDL_free    free
#define SDL_realloc realloc

#define SDL_STBIMAGE_IMPLEMENTATION
#include "SDL_stbimage.h"

SDL_Surface* IMG_Load_RW(SDL_RWops *src, int freesrc) {
  assert(src->type == RW_TYPE_MEM);
  assert(freesrc == 0);
  return NULL;
}

SDL_Surface* IMG_Load(const char *filename) {
  // open the file and get its size
  FILE *fp = fopen(filename, "rb");
  assert(fp != NULL);
  fseek(fp, 0, SEEK_END);
  int size = ftell(fp);
  fseek(fp, 0, SEEK_SET);

  // allocate a buffer of size bytes and read the whole file into it
  void *buf = malloc(size);
  assert(buf != NULL);
  size_t nread = fread(buf, 1, size, fp);
  assert(nread == (size_t)size);

  // decode the image from the buffer into an SDL_Surface
  SDL_Surface *surf = STBIMG_LoadFromMemory(buf, size);

  // close the file and free the buffer
  fclose(fp);
  free(buf);

  return surf;
}

int IMG_isPNG(SDL_RWops *src) {
  return 0;
}

SDL_Surface* IMG_LoadJPG_RW(SDL_RWops *src) {
  return IMG_Load_RW(src, 0);
}

char *IMG_GetError() {
  return "Navy does not support IMG_GetError()";
}
