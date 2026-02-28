#include "common.h"
#include "Vtop.h"
#include "verilated.h"
#include <Vtop__Dpi.h>
class CPU
{
    public:
    CPU(){
        contextp = new VerilatedContext;
        contextp->commandArgs(argc, argv);
        top = new Vtop{contextp};
    }
private:
    VerilatedContext *contextp= nullptr;
    Vtop *top = nullptr;

};
 