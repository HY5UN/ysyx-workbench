#include <proc.h>
#include <elf.h>
#include <fs.h>

#ifdef __LP64__
# define Elf_Ehdr Elf64_Ehdr
# define Elf_Phdr Elf64_Phdr
#else
# define Elf_Ehdr Elf32_Ehdr
# define Elf_Phdr Elf32_Phdr
#endif

static uintptr_t loader(PCB *pcb, const char *filename) {
  // load the ELF executable stored as a file in the ramdisk
  printf("Loading ELF file %s\n", filename);

  int fd = fs_open(filename, 0, 0);

  Elf_Ehdr ehdr;
  fs_read(fd, &ehdr, sizeof(ehdr));

  // validate the ELF magic: 0x7f 'E' 'L' 'F'
  if (memcmp(ehdr.e_ident, ELFMAG, SELFMAG) != 0) {
    panic("Invalid ELF file: bad magic");
  }

  // load every PT_LOAD segment to its virtual address
  for (int i = 0; i < ehdr.e_phnum; i++) {
    Elf_Phdr phdr;
    fs_lseek(fd, ehdr.e_phoff + i * ehdr.e_phentsize, SEEK_SET);
    fs_read(fd, &phdr, sizeof(phdr));
    if (phdr.p_type != PT_LOAD) continue;

    fs_lseek(fd, phdr.p_offset, SEEK_SET);
    fs_read(fd, (void *)phdr.p_vaddr, phdr.p_filesz);
    // zero the [FileSiz, MemSiz) part, e.g. .bss
    memset((void *)(phdr.p_vaddr + phdr.p_filesz), 0,
        phdr.p_memsz - phdr.p_filesz);
  }

  fs_close(fd);
  return ehdr.e_entry;
}

void naive_uload(PCB *pcb, const char *filename) {
  uintptr_t entry = loader(pcb, filename);
  Log("Jump to entry = %p", entry);
  ((void(*)())entry) ();
}
