#include <common.h>

#if defined(MULTIPROGRAM) && !defined(TIME_SHARING)
# define MULTIPROGRAM_YIELD() yield()
#else
# define MULTIPROGRAM_YIELD()
#endif

#define NAME(key) \
  [AM_KEY_##key] = #key,

static const char *keyname[256] __attribute__((used)) = {
  [AM_KEY_NONE] = "NONE",
  AM_KEYS(NAME)
};

static int screen_w = 0, screen_h = 0;

static inline int screen_width() { return screen_w; }
static inline int screen_height() { return screen_h; }

size_t serial_write(const void *buf, size_t offset, size_t len) {
  // MULTIPROGRAM_YIELD();
  for (size_t i = 0; i < len; i++) {
    putch(((const char *)buf)[i]);
  }
  return len;
}

size_t events_read(void *buf, size_t offset, size_t len) {
  // MULTIPROGRAM_YIELD();
  AM_INPUT_KEYBRD_T kb = io_read(AM_INPUT_KEYBRD);
  if (kb.keycode == AM_KEY_NONE) return 0;
  int n = snprintf(buf, len, "%s %s\n", kb.keydown ? "kd" : "ku",
                   keyname[kb.keycode]);
  return n < 0 ? 0 : (size_t)n;
}

size_t dispinfo_read(void *buf, size_t offset, size_t len) {
  AM_GPU_CONFIG_T cfg = io_read(AM_GPU_CONFIG);
  char info[64];
  int n = snprintf(info, sizeof(info), "WIDTH:%d\nHEIGHT:%d\n",
                   cfg.width, cfg.height);
  if (offset >= (size_t)n) return 0;
  if (offset + len > (size_t)n) len = n - offset;
  memcpy(buf, info + offset, len);
  return len;
}

size_t fb_write(const void *buf, size_t offset, size_t len) {
  MULTIPROGRAM_YIELD();
  int w = screen_width();
  const uint32_t *pix = (const uint32_t *)buf;
  size_t npix = len / 4;
  size_t pos = offset / 4;  // pixel index of the first pixel
  while (npix > 0) {
    int x = pos % w;
    int y = pos / w;
    int n = w - x;  // pixels left in this row
    if ((size_t)n > npix) n = npix;
    io_write(AM_GPU_FBDRAW, x, y, (void *)pix, n, 1, true);
    pix += n;
    pos += n;
    npix -= n;
  }
  return len;
}

void init_device() {
  Log("Initializing devices...");
  ioe_init();
  AM_GPU_CONFIG_T cfg = io_read(AM_GPU_CONFIG);
  screen_w = cfg.width;
  screen_h = cfg.height;
  Log("Screen: %d x %d", screen_w, screen_h);
}
