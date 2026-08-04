#include <am.h>
#include <klib.h>
#include <klib-macros.h>
#include <stdarg.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

int printf(const char *fmt, ...)
{
  size_t buf_size = 2048;
  char buf[buf_size];
  va_list ap;
  va_start(ap, fmt);
  int ret = vsnprintf(buf, buf_size, fmt, ap);
  va_end(ap);
  for (char *i = buf; *i; i++)
  {
    putch(*i);
  }
  return ret;
}

int vsprintf(char *out, const char *fmt, va_list ap)
{
  return vsnprintf(out, (size_t)-1, fmt, ap);
}

int sprintf(char *out, const char *fmt, ...)
{
  va_list ap;
  va_start(ap, fmt);
  int ret = vsprintf(out, fmt, ap);
  va_end(ap);
  return ret;
}

int snprintf(char *out, size_t n, const char *fmt, ...)
{
  panic("Not implemented");
}
static void putc_bounded(char *out, size_t size, size_t *n, char c)
{
  if (out && *n + 1 < size)
    out[*n] = c;
  (*n)++;
}

int vsnprintf(char *out, size_t size, const char *fmt, va_list ap)
{
  size_t n = 0;

  while (*fmt)
  {
    if (*fmt != '%')
    {
      putc_bounded(out, size, &n, *fmt++);
      continue;
    }
    fmt++;

    int zero_pad = 0;
    int width = 0;
    if (*fmt == '0')
    {
      zero_pad = 1;
      fmt++;
    }
    while (*fmt >= '0' && *fmt <= '9')
      width = width * 10 + (*fmt++ - '0');

    char spec = *fmt++;
    switch (spec)
    {
    case 's':
    {
      char *str = va_arg(ap, char *);
      if (!str)
        str = "(null)";
      while (*str)
        putc_bounded(out, size, &n, *str++);
      break;
    }
    case 'd':
    case 'i':
    {
      long long num = (long long)va_arg(ap, int);
      int neg = num < 0;
      char tmp[21];
      int i = 0;
      if (neg)
        num = -num;
      do
      {
        tmp[i++] = (char)(num % 10 + '0');
        num /= 10;
      } while (num);
      while (i < width)
        tmp[i++] = (char)(zero_pad ? '0' : ' ');
      if (neg)
        putc_bounded(out, size, &n, '-');
      while (i > 0)
        putc_bounded(out, size, &n, tmp[--i]);
      break;
    }
    case 'c':
    {
      putc_bounded(out, size, &n, (char)va_arg(ap, int));
      break;
    }
    case 'x':
    case 'X':
    {
      unsigned int num = va_arg(ap, unsigned int);
      int upper = spec == 'X';
      char tmp[9];
      int i = 0;
      if (!num)
        tmp[i++] = '0';
      while (num)
      {
        int d = (int)(num & 0xF);
        tmp[i++] = (char)(d < 10 ? d + '0' : d - 10 + (upper ? 'A' : 'a'));
        num >>= 4;
      }
      while (i < width)
        tmp[i++] = (char)(zero_pad ? '0' : ' ');
      while (i > 0)
        putc_bounded(out, size, &n, tmp[--i]);
      break;
    }
    case 'p':
    {
      uintptr_t ptr = (uintptr_t)va_arg(ap, void *);
      char tmp[9];
      int i = 0;
      if (!ptr)
        tmp[i++] = '0';
      while (ptr)
      {
        int d = (int)(ptr & 0xF);
        tmp[i++] = (char)(d < 10 ? d + '0' : d - 10 + 'a');
        ptr >>= 4;
      }
      while (i < width)
        tmp[i++] = (char)(zero_pad ? '0' : ' ');
      putc_bounded(out, size, &n, '0');
      putc_bounded(out, size, &n, 'x');
      while (i > 0)
        putc_bounded(out, size, &n, tmp[--i]);
      break;
    }
    default:
      break;
    }
  }

  if (out && size > 0)
    out[n < size - 1 ? n : size - 1] = '\0';
  return (int)n;
}

#endif
