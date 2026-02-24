#include <klib.h>
#include <klib-macros.h>
#include <stdint.h>

#if !defined(__ISA_NATIVE__) || defined(__NATIVE_USE_KLIB__)

size_t strlen(const char *s) {
  panic("Not implemented");
}

char *strcpy(char *dst, const char *src) {
  //panic("Not implemented");
  char *ptr = dst;
  while(*src){
    *ptr++=*src++;
  }
  *ptr = '\0';
  return dst;

}

char *strncpy(char *dst, const char *src, size_t n) {
  panic("Not implemented");
}

char *strcat(char *dst, const char *src) {
  //panic("Not implemented");
  char *ptr=dst;
  while(*ptr){
    ptr++;
  }
  while(*src){
    *ptr++=*src++;
  }
  *ptr='\0';
  return dst;

}

int strcmp(const char *s1, const char *s2) {
  //panic("Not implemented");
  while (*s1 && *s2 && *s1 == *s2) {
    s1++;
    s2++;
  }
  return (unsigned char)*s1 - (unsigned char)*s2;

}

int strncmp(const char *s1, const char *s2, size_t n) {
  panic("Not implemented");
}

void *memset(void *s, int c, size_t n) {
  //panic("Not implemented");
  unsigned char *ptr=s;
  for (int i=0;i<n;i++){
    ptr[i]=c;
  }

  return s;
}

void *memmove(void *dst, const void *src, size_t n) {
  panic("Not implemented");

}

void *memcpy(void *out, const void *in, size_t n) {
  panic("Not implemented");
}

int memcmp(const void *s1, const void *s2, size_t n) {
  //panic("Not implemented");
  const unsigned char *p1=s1;
  const unsigned char *p2=s2;


  for (int i=0;i<n;i++){
    if(p1[i]!=p2[i]){
      return p1[i]-p2[i];
    }

  }
  return 0;

}

#endif
