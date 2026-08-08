#include <fs.h>

typedef size_t (*ReadFn) (void *buf, size_t offset, size_t len);
typedef size_t (*WriteFn) (const void *buf, size_t offset, size_t len);

typedef struct {
  char *name;
  size_t size;
  size_t disk_offset;
  ReadFn read;
  WriteFn write;
  size_t open_offset;  // current read/write position of the opened file
} Finfo;

enum {FD_STDIN, FD_STDOUT, FD_STDERR, FD_FB};

size_t ramdisk_read(void *buf, size_t offset, size_t len);
size_t ramdisk_write(const void *buf, size_t offset, size_t len);
size_t serial_write(const void *buf, size_t offset, size_t len);

/* reads from stdin/stdout/stderr are ignored for now */
size_t stdio_read(void *buf, size_t offset, size_t len) {
  return 0;
}

/* writes to stdin are ignored for now */
size_t stdio_write(const void *buf, size_t offset, size_t len) {
  return 0;
}

/* This is the information about all files in disk. */
static Finfo file_table[] __attribute__((used)) = {
  [FD_STDIN]  = {"stdin", 0, 0, stdio_read, stdio_write},
  [FD_STDOUT] = {"stdout", 0, 0, stdio_read, serial_write},
  [FD_STDERR] = {"stderr", 0, 0, stdio_read, serial_write},
#include "files.h"
};

#define NR_FILES (sizeof(file_table) / sizeof(file_table[0]))

int fs_open(const char *pathname, int flags, int mode) {
  for (size_t i = 0; i < NR_FILES; i++) {
    if (strcmp(file_table[i].name, pathname) == 0) {
      file_table[i].open_offset = 0;
      return i;
    }
  }
  panic("fs_open: cannot find file '%s'", pathname);
  return -1;
}

size_t fs_read(int fd, void *buf, size_t len) {
  Finfo *file = &file_table[fd];
  size_t ret;
  if (file->read) {
    ret = file->read(buf, file->open_offset, len);
  } else {
    size_t remain = (file->open_offset < file->size) ?
                    file->size - file->open_offset : 0;
    if (len > remain) len = remain;
    ret = ramdisk_read(buf, file->disk_offset + file->open_offset, len);
  }
  file->open_offset += ret;
  return ret;
}

size_t fs_write(int fd, const void *buf, size_t len) {
  Finfo *file = &file_table[fd];
  size_t ret;
  if (file->write) {
    ret = file->write(buf, file->open_offset, len);
  } else {
    size_t remain = (file->open_offset < file->size) ?
                    file->size - file->open_offset : 0;
    if (len > remain) len = remain;
    ret = ramdisk_write(buf, file->disk_offset + file->open_offset, len);
  }
  file->open_offset += ret;
  return ret;
}

size_t fs_lseek(int fd, size_t offset, int whence) {
  Finfo *file = &file_table[fd];
  switch (whence) {
    case SEEK_SET: file->open_offset = offset; break;
    case SEEK_CUR: file->open_offset += offset; break;
    case SEEK_END: file->open_offset = file->size + offset; break;
    default: panic("fs_lseek: invalid whence %d", whence);
  }
  return file->open_offset;
}

int fs_close(int fd) {
  return 0;
}

void init_fs() {
  // TODO: initialize the size of /dev/fb
}
