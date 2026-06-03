#include "include/common.h"
#include "include/trace.h"
#include "include/config.h"
#include "verilated_fst_c.h"
#include "include/CPU.h"

#ifdef ENABLE_FST
static VerilatedFstC *tfp = nullptr;
static uint64_t sim_time = 0;
#endif

void fst_init(Vtop *top)
{
#ifdef ENABLE_FST
    Verilated::traceEverOn(true);
    tfp = new VerilatedFstC;
    top->trace(tfp, 99); // 99 = trace 深度（层数）
    tfp->open("waveform.fst");

#endif
}

bool trace_latest = true;
long long trace_start_time = 0;

int file_cnt = 0;//tmp
void fst_dump_once()
{
    //printf("sim_time: %lld\n", sim_time);//tmp
#ifdef ENABLE_FST
    if(trace_start_time-->0){
        return;
    }
    if (sim_time < MAX_SIM_TIME)
    {
        tfp->dump(sim_time);
        sim_time++;
    }
    else if (trace_latest)
    {
        sim_time = 0;
        fst_close();
        fst_init(cpu->top);
        tfp->dump(sim_time);
        sim_time++;

        file_cnt++;//tmp
        // printf("file_cnt: %d\n", file_cnt);//tmp


    }
    else {
        fst_close();
    }
#endif
}

void fst_close()
{
#ifdef ENABLE_FST
    if (tfp)
    {
        tfp->close();
        delete tfp;
        tfp = nullptr;
    }
#endif
}