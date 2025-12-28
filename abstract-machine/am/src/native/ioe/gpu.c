#include <am.h>
#include <SDL.h>
#include <fenv.h>

//#define MODE_800x600
#define WINDOW_W 800
#define WINDOW_H 600
#ifdef MODE_800x600
const int disp_w = WINDOW_W, disp_h = WINDOW_H;
#else
const int disp_w = 400, disp_h = 300;
#endif

// 移除 Timer 相关的定义，改为手动计算帧率
// #define FPS   60  <-- 不再需要硬编码的 FPS 宏作为 Timer 参数

#define RMASK 0x00ff0000
#define GMASK 0x0000ff00
#define BMASK 0x000000ff
#define AMASK 0x00000000

static SDL_Window *window = NULL;
static SDL_Surface *surface = NULL;

// [已删除] static Uint32 texture_sync ... (这是导致死锁的元凶)

void __am_gpu_init() {
  SDL_Init(SDL_INIT_VIDEO | SDL_INIT_TIMER);
  
  // 注意：虽然之前写了 OPENGL，但你实际用的是软件渲染 API (UpdateWindowSurface)
  // 为了稳妥，这里保持原样，但去掉 Timer
  window = SDL_CreateWindow("Native Application",
      SDL_WINDOWPOS_UNDEFINED, SDL_WINDOWPOS_UNDEFINED,
      WINDOW_W, WINDOW_H, SDL_WINDOW_OPENGL);
      
  surface = SDL_CreateRGBSurface(SDL_SWSURFACE, disp_w, disp_h, 32,
      RMASK, GMASK, BMASK, AMASK);
      
  // [已删除] SDL_AddTimer(1000 / FPS, texture_sync, NULL); 
}

void __am_gpu_config(AM_GPU_CONFIG_T *cfg) {
  *cfg = (AM_GPU_CONFIG_T) {
    .present = true, .has_accel = false,
    .width = disp_w, .height = disp_h,
    .vmemsz = 0
  };
}

void __am_gpu_status(AM_GPU_STATUS_T *stat) {
  stat->ready = true;
}

void __am_gpu_fbdraw(AM_GPU_FBDRAW_T *ctl) {
  int x = ctl->x, y = ctl->y, w = ctl->w, h = ctl->h;
  
  // 1. 先处理绘图数据 (写入内部 surface)
  if (w > 0 && h > 0) {
      feclearexcept(-1);
      SDL_Surface *s = SDL_CreateRGBSurfaceFrom(ctl->pixels, w, h, 32, w * sizeof(uint32_t),
          RMASK, GMASK, BMASK, AMASK);
      SDL_Rect rect = { .x = x, .y = y };
      SDL_BlitSurface(s, NULL, surface, &rect);
      SDL_FreeSurface(s);
  }

  // 2. 关键修复：当需要同步(sync=true)时，在主线程直接刷新屏幕
  if (ctl->sync) {
      // 将内部的小分辨率 surface 缩放 blit 到 窗口 surface
      SDL_BlitScaled(surface, NULL, SDL_GetWindowSurface(window), NULL);
      SDL_UpdateWindowSurface(window);

      // --- 手动限速 (VSync 模拟) ---
      // 防止刷新太快再次把驱动冲垮 (Intel Arc 在 WSL 下很脆弱)
      // static Uint32 last_tick = 0;
      // Uint32 current_tick = SDL_GetTicks();
      //printf("%d ",current_tick);
      // 目标 60 FPS = 16ms 一帧
      // if (current_tick - last_tick < 16) {
      //     SDL_Delay(16 - (current_tick - last_tick));
      // }
      // last_tick = SDL_GetTicks();

  }
}