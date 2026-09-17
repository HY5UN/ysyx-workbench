#include <am.h>
#include <nemu.h>
#include <klib.h>

static AddrSpace kas = {};
static void *(*pgalloc_usr)(int) = NULL;
static void (*pgfree_usr)(void *) = NULL;
static int vme_enable = 0;

static Area segments[] = { // Kernel memory mappings
    NEMU_PADDR_SPACE};

#define USER_SPACE RANGE(0x40000000, 0x80000000)

static inline void set_satp(void *pdir)
{
  uintptr_t mode = 1ul << (__riscv_xlen - 1);
  asm volatile("csrw satp, %0" : : "r"(mode | ((uintptr_t)pdir >> 12)));
}

static inline uintptr_t get_satp()
{
  uintptr_t satp;
  asm volatile("csrr %0, satp" : "=r"(satp));
  return satp << 12;
}

bool vme_init(void *(*pgalloc_f)(int), void (*pgfree_f)(void *))
{
  pgalloc_usr = pgalloc_f;
  pgfree_usr = pgfree_f;

  kas.ptr = pgalloc_f(PGSIZE);
  printf("kas pdir set as kas.ptr=%p\n",kas.ptr);

  int i;
  for (i = 0; i < LENGTH(segments); i++)
  {
    void *va = segments[i].start;
    for (; va < segments[i].end; va += PGSIZE)
    {
      map(&kas, va, va, 0);
    }
  }

  set_satp(kas.ptr);
  vme_enable = 1;

  return true;
}

void protect(AddrSpace *as)
{
  PTE *updir = (PTE *)(pgalloc_usr(PGSIZE));
  printf("protect: updir = %p\n", updir);
  as->ptr = updir;
  as->area = USER_SPACE;
  as->pgsize = PGSIZE;
  // map kernel space
  memcpy(updir, kas.ptr, PGSIZE);
}

void unprotect(AddrSpace *as)
{
}

void __am_get_cur_as(Context *c)
{
  c->pdir = (vme_enable ? (void *)get_satp() : NULL);
}

void __am_switch(Context *c)
{
  if (vme_enable && c->pdir != NULL)
  {
    set_satp(c->pdir);
  }
}

void map(AddrSpace *as, void *va, void *pa, int prot)
{
  
  uintptr_t pd_idx = (uintptr_t)va >> 22;
  uintptr_t pt_idx = ((uintptr_t)va >> 12) & 0x3FF;
  PTE *pd = (PTE *)as->ptr;

  if (!pd[pd_idx] & PTE_V)
  {
    PTE new_p = (PTE)(pgalloc_usr(PGSIZE));
    // printf("map: new_p = %p, pd_idx = %d\n", new_p, pd_idx);
    pd[pd_idx] = new_p >> 12 << 10;
    pd[pd_idx] |= PTE_V;
  }

  PTE *pt = (PTE *)(pd[pd_idx] >> 10 << 12);
  pt[pt_idx] = (PTE)pa >> 12 << 10;
  pt[pt_idx] |= PTE_V;

  // if((uintptr_t)(pt + pt_idx) == 0x81e7b150){
  //   printf("map: pt_idx == 0x81e7b150, va = %p, pa = %p, pt = %p, pt_idx = %d, pd_idx = %d\n", va, pa, pt, pt_idx, pd_idx);
  // }

  // if(ROUNDDOWN(0x40054f18, PGSIZE) == ROUNDDOWN(va, PGSIZE)||
  //    ROUNDDOWN(0x40100150, PGSIZE) == ROUNDDOWN(va, PGSIZE)){
  //   printf("map: va = %p, pa = %p, pt = %p, pt_idx = %d, pd_idx = %d\n", va, pa, pt, pt_idx, pd_idx);
  // }
}

Context *ucontext(AddrSpace *as, Area kstack, void *entry)
{
  // 与 kcontext() 类似: 在内核栈顶分配 Context, 只需设置入口地址.
  // 用户进程不需要传递参数 (arg), 栈顶由 Nanos-lite 通过 GPRx 约定给出,
  // as 参数在实现分页(PA4.3)之前忽略
  Context *cp = kstack.end - sizeof(Context);
  cp->mepc = (uintptr_t)entry;
  cp->pdir = as->ptr;
  // cp->gpr[2] = (uintptr_t)as->area.end; // set sp
  return cp;
}
