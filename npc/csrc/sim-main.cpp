#include "Vtop.h"
#include "verilated.h"

int main(int argc, char **argv) {
  VerilatedContext *contextp = new VerilatedContext;
  contextp->commandArgs(argc, argv);
  Vtop *top = new Vtop{contextp};
  
  top->clock = 0;
  top->reset = 1;
  top->eval();
  contextp->timeInc(1);
  
  top->reset = 0;
  
  while (!contextp->gotFinish()) {
    top->clock = !top->clock;
    top->eval();
    contextp->timeInc(1);
  }

  delete top;
  delete contextp;
  return 0;
}