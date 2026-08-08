#include <stdint.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <assert.h>
#include <unistd.h>
#include <fcntl.h>
#include <sys/time.h>

static int evtdev = -1;
static int fbdev = -1;
static int screen_w = 0, screen_h = 0;
static int canvas_w = 0, canvas_h = 0;
static int canvas_x = 0, canvas_y = 0;  // position of the canvas on the screen

uint32_t NDL_GetTicks() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  return (uint32_t)tv.tv_sec * 1000 + (uint32_t)tv.tv_usec / 1000;
}

int NDL_PollEvent(char *buf, int len) {
  int nread = read(evtdev, buf, len);
  if (nread <= 0) return 0;
  return 1;
}

void NDL_OpenCanvas(int *w, int *h) {
  if (getenv("NWM_APP")) {
    int fbctl = 4;
    fbdev = 5;
    screen_w = *w; screen_h = *h;
    char buf[64];
    int len = sprintf(buf, "%d %d", screen_w, screen_h);
    // let NWM resize the window and create the frame buffer
    write(fbctl, buf, len);
    while (1) {
      // 3 = evtdev
      int nread = read(3, buf, sizeof(buf) - 1);
      if (nread <= 0) continue;
      buf[nread] = '\0';
      if (strcmp(buf, "mmap ok") == 0) break;
    }
    close(fbctl);
  }
  if (*w == 0 && *h == 0) {
    *w = screen_w;
    *h = screen_h;
  }
  assert(*w <= screen_w && *h <= screen_h);
  canvas_w = *w;
  canvas_h = *h;
  canvas_x = (screen_w - canvas_w) / 2;
  canvas_y = (screen_h - canvas_h) / 2;
}

void NDL_DrawRect(uint32_t *pixels, int x, int y, int w, int h) {
  for (int j = 0; j < h; j++) {
    int row = canvas_y + y + j;
    int col = canvas_x + x;
    lseek(fbdev, (row * screen_w + col) * 4, SEEK_SET);
    write(fbdev, pixels + j * w, w * 4);
  }
}

void NDL_OpenAudio(int freq, int channels, int samples) {
}

void NDL_CloseAudio() {
}

int NDL_PlayAudio(void *buf, int len) {
  return 0;
}

int NDL_QueryAudio() {
  return 0;
}

int NDL_Init(uint32_t flags) {
  if (getenv("NWM_APP")) {
    evtdev = 3;
  } else {
    evtdev = open("/dev/events", 0, 0);
    fbdev = open("/dev/fb", 0, 0);
    int dispinfo = open("/proc/dispinfo", 0, 0);
    char buf[128];
    int n = read(dispinfo, buf, sizeof(buf) - 1);
    close(dispinfo);
    buf[n] = '\0';
    sscanf(buf, "WIDTH:%d\nHEIGHT:%d", &screen_w, &screen_h);
  }
  return 0;
}

void NDL_Quit() {
}
