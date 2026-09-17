#include <common.h>

extern void do_syscall(Context *c);
extern Context* schedule(Context *prev);

static Context* do_event(Event e, Context* c) {
  switch (e.event) {
    case EVENT_YIELD:
      // Log("Yielding from process %p", c);
      return schedule(c);
    case EVENT_SYSCALL:
      do_syscall(c);
      break;
    case EVENT_ERROR:
      panic("Event error, cause %d\n", e.cause);
      break;
    default: panic("Unhandled event ID = %d", e.event);
  }

  return c;
}

void init_irq(void) {
  Log("Initializing interrupt/exception handler...");
  cte_init(do_event);
}
