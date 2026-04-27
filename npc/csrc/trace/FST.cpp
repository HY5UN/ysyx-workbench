#include "include/common.h"
#include "include/trace.h"
#include "include/config.h"
#include "verilated_fst_c.h"


#ifdef ENABLE_FST
static VerilatedFstC *tfp = nullptr;
static uint64_t sim_time = 0;
#endif

void fst_init()
{
#ifdef ENABLE_FST
    Verilated::traceEverOn(true);
    tfp = new VerilatedFstC;
    top->trace(tfp, 99); // 99 = trace 深度（层数）
    tfp->open("waveform.fst");

#endif
}

void fst_dump_once()
{
#ifdef ENABLE_FST

    if (sim_time < MAX_SIM_TIME)
    {
        tfp->dump(sim_time);
        sim_time++;
    }
    else
    {
        tfp->close();
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