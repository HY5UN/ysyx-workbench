#include <proc.h>
#include <elf.h>
#include <fs.h>

#ifdef __LP64__
#define Elf_Ehdr Elf64_Ehdr
#define Elf_Phdr Elf64_Phdr
#else
#define Elf_Ehdr Elf32_Ehdr
#define Elf_Phdr Elf32_Phdr
#endif

uintptr_t loader(PCB *pcb, const char *filename)
{
  // load the ELF executable stored as a file in the ramdisk
  printf("Loading ELF file %s\n", filename);

  int fd = fs_open(filename, 0, 0);
  // callers (sys_execve, proc.c, sys_exit) are expected to pass an
  // existing file; if not, panic instead of reading with a bad fd
  if (fd < 0)
  {
    panic("loader: cannot open '%s'", filename);
  }

  Elf_Ehdr ehdr;
  fs_read(fd, &ehdr, sizeof(ehdr));

  // validate the ELF magic: 0x7f 'E' 'L' 'F'
  if (memcmp(ehdr.e_ident, ELFMAG, SELFMAG) != 0)
  {
    panic("Invalid ELF file: bad magic");
  }

  uintptr_t brk = 0;

  // load every PT_LOAD segment to its virtual address
  for (int i = 0; i < ehdr.e_phnum; i++)
  {
    Elf_Phdr phdr;
    fs_lseek(fd, ehdr.e_phoff + i * ehdr.e_phentsize, SEEK_SET);
    fs_read(fd, &phdr, sizeof(phdr));
    if (phdr.p_type != PT_LOAD)
      continue;

    fs_lseek(fd, phdr.p_offset, SEEK_SET);

    uintptr_t begin_va = ROUNDDOWN(phdr.p_vaddr, PGSIZE);
    uintptr_t end_va = ROUNDUP(phdr.p_vaddr + phdr.p_memsz, PGSIZE);
    brk = end_va > brk ? end_va : brk;

    size_t remain_size = phdr.p_filesz;
    for (uintptr_t va = begin_va; va < end_va; va += PGSIZE)
    {
      uintptr_t pa = (uintptr_t)new_page(1);
      int prot = 0;
      map(&pcb->as, (void *)va, (void *)pa, prot);
      if (va == begin_va)
      {
        memset((void *)pa, 0, PGSIZE);
        uintptr_t offset = phdr.p_vaddr - begin_va;
        size_t read_size = phdr.p_filesz > PGSIZE - offset ? PGSIZE - offset : phdr.p_filesz;
        fs_read(fd, (void *)(pa + offset), read_size);
        remain_size -= read_size;
        continue;
      }
      memset((void *)pa, 0, PGSIZE);
      if (va < phdr.p_vaddr + phdr.p_filesz)
      {
        size_t read_size = remain_size < PGSIZE ? remain_size : PGSIZE;
        fs_read(fd, (void *)pa, read_size);
        remain_size -= read_size;
        continue;
      }
    }
  }

  fs_close(fd);
  pcb->max_brk = brk;
  printf("ELF file %s loaded, entry point = %p\n", filename, (void *)ehdr.e_entry);
  return ehdr.e_entry;
}

void naive_uload(PCB *pcb, const char *filename)
{
  uintptr_t entry = loader(pcb, filename);
  Log("Jump to entry = %p", entry);
  ((void (*)())entry)();
}
