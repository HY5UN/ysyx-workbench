#include "Vtop.h"
#include"verilated.h"
#include"verilated_fst_c.h"

#include<stdio.h>
#include<stdlib.h>
#include<assert.h>

int main(int argc, char** argv){
    VerilatedContext* contextp =new VerilatedContext;
    contextp->commandArgs(argc,argv);
    contextp->traceEverOn(true);
    
    Vtop* top = new Vtop{contextp};

    VerilatedFstC* tfp = new VerilatedFstC;
    top->trace(tfp,99);
    tfp->open("waveform.fst");

    while(!contextp->gotFinish()){
        int a=rand()&1;
        int b=rand()&1;

        top->a=a;
        top->b=b;

        top->eval();
        tfp->dump(contextp->time());
        printf("a=%d b=%d out=%d\n",a,b,top->f);
        assert((a^b) == top->f);

        if(contextp->time() >20) break;
        contextp->timeInc(1);
    }

    delete top;
    delete contextp;
    delete tfp;
    return 0;
}

