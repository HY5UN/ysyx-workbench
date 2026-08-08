#include <stdio.h>
#include <NDL.h>

int main() {
  NDL_Init(0);
  uint32_t last = NDL_GetTicks();
  printf("Timer start: %u ms\n", (unsigned)last);
  while (1) {
    uint32_t now = NDL_GetTicks();
    if (now - last >= 500) {
      printf("Timer: %u ms\n", (unsigned)now);
      last = now;
    }
  }
  return 0;
}
