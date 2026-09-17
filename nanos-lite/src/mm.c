#include <memory.h>
#include <proc.h>

static void *pf = NULL;

void *new_page(size_t nr_page)
{
  void *p = pf;
  pf = (void *)((uintptr_t)pf + nr_page * PGSIZE);
  return p;
}

#ifdef HAS_VME
static void *pg_alloc(int n)
{
  void *p = pf;
  for (; n > 0; n -= PGSIZE)
  {
    new_page(1);
  }
  for (void *i = p; i < pf; i += 4)
  {
    *(uint32_t *)i = 0;
  }
  return p;
}
#endif

void free_page(void *p)
{
  panic("not implement yet");
}

/* The brk() system call handler. */
int mm_brk(uintptr_t brk)
{
  // printf("mm_brk: brk = 0x%08x, current->max_brk = 0x%08x\n", brk, current->max_brk);
  if (current->max_brk >= brk)
    return 0;

  uintptr_t curr_end = ROUNDUP(current->max_brk, PGSIZE);
  uintptr_t new_end = ROUNDUP(brk, PGSIZE);
  // printf("mm_brk: mapping new pages from 0x%08x to 0x%08x\n", curr_end, new_end);
  for (uintptr_t va = curr_end; va < new_end; va += PGSIZE)
  {
    void *pa = new_page(1);
    int prot = 0;
    map(&current->as, (void *)va, pa, prot);
  }
  current->max_brk = new_end;

  return 0;
}

void init_mm()
{
  pf = (void *)ROUNDUP(heap.start, PGSIZE);
  Log("free physical pages starting from %p", pf);

#ifdef HAS_VME
  vme_init(pg_alloc, free_page);
#endif
}
