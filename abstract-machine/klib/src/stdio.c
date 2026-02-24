#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...) {
  //panic("Not implemented");
  char buf[1024];
  va_list ap;
  va_start(ap, fmt);
  int ret = vsprintf(buf, fmt, ap);
  va_end(ap);
  for (char *i = buf; *i; i++) {
    putch(*i);
  }
  return ret;
}

int vsprintf(char *out, const char *fmt, va_list ap) {
  char *ptr = out;
  while (*fmt) {
    if (*fmt != '%') {
      *ptr++ = *fmt++;
      continue;
    }
    fmt++;
    switch (*fmt++)
    {
    case 's': {
      char *str = va_arg(ap, char *);
      while (*str) {
        *ptr++ = *str++;
      }
      break;
    }
    case 'd':case 'i': {
      int inum = va_arg(ap, int);
      long long num = (long long)inum;
      if (num < 0) {
        *ptr++ = '-';
        num = -num;
      }
      char buf[21];
      int i = 0;
      do {
        buf[i++] = num % 10 + '0';
        num /= 10;
      } while (num);
      while (i--) {
        *ptr++ = buf[i];
      }
      break;
    }
    default:
      break;
    }
  }
  *ptr = '\0';
  return ptr - out;
}

int sprintf(char *out, const char *fmt, ...) {
  va_list ap;
  va_start(ap, fmt);
  int ret = vsprintf(out, fmt, ap);
  va_end(ap);
  return ret;
}

int snprintf(char *out, size_t n, const char *fmt, ...) {
  panic("Not implemented");
}

int vsnprintf(char *out, size_t n, const char *fmt, va_list ap) {
  panic("Not implemented");
}

#endif
