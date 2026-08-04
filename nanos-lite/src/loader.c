#include <proc.h>
#include <elf.h>

#ifdef __LP64__
# define Elf_Ehdr Elf64_Ehdr
# define Elf_Phdr Elf64_Phdr
#else
# define Elf_Ehdr Elf32_Ehdr
# define Elf_Phdr Elf32_Phdr
#endif

/* ramdisk access functions, see ramdisk.c */
size_t ramdisk_read(void *buf, size_t offset, size_t len);
size_t get_ramdisk_size();

static uintptr_t loader(PCB *pcb, const char *filename) {
  // the ramdisk currently holds a single ELF executable, starting at offset 0
  Elf_Ehdr ehdr;
  ramdisk_read(&ehdr, 0, sizeof(ehdr));

  // validate the ELF magic: 0x7f 'E' 'L' 'F'
  if (memcmp(ehdr.e_ident, ELFMAG, SELFMAG) != 0) {
    panic("Invalid ELF file: bad magic");
  }

  // load every PT_LOAD segment to its virtual address
  for (int i = 0; i < ehdr.e_phnum; i++) {
    Elf_Phdr phdr;
    ramdisk_read(&phdr, ehdr.e_phoff + i * ehdr.e_phentsize, sizeof(phdr));
    if (phdr.p_type != PT_LOAD) continue;

    ramdisk_read((void *)phdr.p_vaddr, phdr.p_offset, phdr.p_filesz);
    // zero the [FileSiz, MemSiz) part, e.g. .bss
    memset((void *)(phdr.p_vaddr + phdr.p_filesz), 0,
        phdr.p_memsz - phdr.p_filesz);
  }

  return ehdr.e_entry;
}

void naive_uload(PCB *pcb, const char *filename) {
  uintptr_t entry = loader(pcb, filename);
  Log("Jump to entry = %p", entry);
  ((void(*)())entry) ();
}
