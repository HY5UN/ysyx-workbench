#include <stdio.h>
#include <sys/time.h>

int main() {
  struct timeval tv;
  gettimeofday(&tv, NULL);
  long long last = (long long)tv.tv_sec * 1000000 + tv.tv_usec;
  printf("Timer start: %d.%06d s\n", (int)(last / 1000000), (int)(last % 1000000));
  while (1) {
    gettimeofday(&tv, NULL);
    long long now = (long long)tv.tv_sec * 1000000 + tv.tv_usec;
    if (now - last >= 500000) {
      printf("Timer: %d.%06d s\n", (int)(now / 1000000), (int)(now % 1000000));
      last = now;
    }
  }
  return 0;
}
