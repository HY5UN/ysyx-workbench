#include <am.h>
#include <riscv/riscv.h>
#include <klib.h>

static Context *(*user_handler)(Event, Context *) = NULL;

extern void __am_get_cur_as(Context *c);
extern void __am_switch(Context *c);

#define INTR 0x80000000

Context *__am_irq_handle(Context *c)
{
  __am_get_cur_as(c);
  // printf("am_get_cur_as: c->pdir = %p\n", c->pdir);
  if (user_handler)
  {
    Event ev = {0};
    uintptr_t ecode = c->mcause & ~INTR;
    if ((c->mcause & INTR) == INTR)
    {
      c->mcause &= ~INTR;
      switch (ecode)
      {
      case 7: // Machine timer interrupt (MTI)
        ev.event = EVENT_IRQ_TIMER;
        // c->mepc+= 4;
        break;

      default:
        ev.event = EVENT_ERROR;
        ev.cause = c->mcause;
        break;
      }
    }
    else
    {
      switch (ecode)
      {
      case 11: // m ecall
        if (c->GPR1 == (uintptr_t)-1)
          ev.event = EVENT_YIELD;
        else
          ev.event = EVENT_SYSCALL;
        c->mepc += 4;
        break;

      default:
        ev.cause = c->mcause;
        ev.event = EVENT_ERROR;
        break;
      }
    }

    c = user_handler(ev, c);
    assert(c != NULL);
  }
  else
  {
    printf("__am_irq_handle: user_handler not found.\n");
  }
  // printf("am_switch: c->pdir = %p\n", c->pdir);
  __am_switch(c);

  return c;
}

extern void __am_asm_trap(void);

bool cte_init(Context *(*handler)(Event, Context *))
{
  // initialize exception entry
  asm volatile("csrw mtvec, %0" : : "r"(__am_asm_trap));
  // ksp = 0: we are running in kernel now
  asm volatile("csrw mscratch, zero");

  // register event handler
  user_handler = handler;

  return true;
}

Context *kcontext(Area kstack, void (*entry)(void *), void *arg)
{
  Context *cp = kstack.end - sizeof(Context);
  // mret copies MPIE to MIE, so set MPIE to run with interrupts enabled
  cp->mstatus = MPIE;
  cp->mepc = (uintptr_t)entry;
  cp->gpr[10] = (uintptr_t)arg;
  cp->gpr[2] = (uintptr_t)kstack.end; // sp after the context is popped
  cp->pdir = NULL;
  cp->np = 0; // kernel thread: stay in kernel, no stack switching
  return cp;
}

void yield()
{
#ifdef __riscv_e
  asm volatile("li a5, -1; ecall");
#else
  asm volatile("li a7, -1; ecall");
#endif
}

bool ienabled()
{
  return false;
}

void iset(bool enable)
{
}
