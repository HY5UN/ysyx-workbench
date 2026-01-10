#include <nvboard.h>
#include <Vtop.h>

static TOP_NAME dut;

void nvboard_bind_all_pins(TOP_NAME* top);

static void single_cycle() {
  dut.clock = 0; dut.eval();
  dut.clock = 1; dut.eval();
}

static void reset() {
  dut.reset = 1; single_cycle();
  dut.reset = 0; single_cycle();
}

int main() {
  nvboard_bind_all_pins(&dut);
  nvboard_init();

  reset();
  // nvboard_update();

  while(1) {
    nvboard_update();
    single_cycle();
    //dut.eval();
  }
}
