#include "include/common.h"
#include "include/trace.h"
#include "include/CPU.h"
#include "include/mem.h"
#include "include/config.h"
#include "verilated_fst_c.h"

static bool dpic_ebreak_triggered = false;
static bool dpic_difftest_step_triggered = false;

void fst_dump_once(VerilatedFstC *tfp, uint64_t &sim_time)
{
    if (sim_time < MAX_SIM_TIME)
    {
        tfp->dump(sim_time);
        sim_time++;
    }
    else
    {
        tfp->close();
    }
}

CPU::CPU(int argc, char **argv)
{
    contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    top = new Vtop{contextp};

#ifdef ENABLE_DIFFTEST
    difftest = new DiffTest();
    difftest->difftest_init(2333);
    difftest->difftest_memcpy(BEGIN_ADDR, memory, bin_size, DIFFTEST_TO_REF);
#endif
#ifdef ENABLE_FST
    Verilated::traceEverOn(true);
    tfp = new VerilatedFstC;
    top->trace(tfp, 99); // 99 = trace 深度（层数）
    tfp->open("waveform.fst");

#endif
}

CPU::~CPU()
{

    delete tfp;

    delete top;
    delete contextp;
}

const char *reg_names[] = {
    "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
    "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5"};

void CPU::reg_print()
{
    uint32_t *addr = (uint32_t *)&top->io_allReg_0;
    for (int i = 0; i < REG_NUM; i++)
    {
        if (i % 8 == 0 && i != 0)
            printf("\n");
        printf("\tx%-2d(%s): 0x%08x ", i, reg_names[i], addr[i]);
    }
    printf("\n");
}

void CPU::reset(int n)
{
    top->reset = 1;
    for (int i = 0; i < n; i++)
    {
        top->clock = 0;
        top->eval();
        top->clock = 1;
        top->eval();
    }
    top->reset = 0;
    top->clock = 0;
    top->eval();

#ifdef ENABLE_FTRACE
    if (ftrace_enabled)
    {
        get_init_func_symbols(top->io_pc);
    }

#endif
}

bool CPU::execute(uint64_t steps)
{
    for (; steps > 0 && !contextp->gotFinish() && !dpic_ebreak_triggered; steps--)
    {
        if (!execute_once())
        {
            printf("CPU execution failed at PC = 0x%08x\n", top->io_pc);
            return false;
        }
    }
    return true;
}

bool CPU::execute_once()
{
#ifdef ENABLE_ITRACE
    // 根据上升沿后的组合逻辑状态来记录，所以写内存操作的记录为上一周期的
    itrace_write(top->io_pc, top->io_inst);
    trace_log();

#endif

#ifdef ENABLE_FTRACE

    if (ftrace_enabled)
    {
        int rd = (top->io_inst >> 7) & 0x1F;
        int rs1 = (top->io_inst >> 15) & 0x1F;
        if (was_jal())
        {
            ftrace_record(top->io_pc, true);
        }
        else if (was_jalr())
        {
            ftrace_record(top->io_pc, false);
        }

        save_prev_state(top->io_pc, top->io_inst, rd, rs1);
    }

#endif

    top->clock = 0;
    top->eval();
    top->clock = 1;
    top->eval();

    contextp->timeInc(1);

    if (dpic_ebreak_triggered)
    {
        std::cout << ">>> 执行 ebreak 指令，触发仿真结束。pc= " << std::hex << top->io_pc << std::dec << std::endl;

#ifdef ENABLE_FST
        tfp->close();
#endif
        if (top->io_allReg_10 == 0)
        {
            std::cout << "HIT GOOD TRAP!" << std::endl;
        }
        else
        {
            reg_print();

            std::cout << "HIT BAD TRAP! x10 = " << std::hex << top->io_allReg_10 << std::dec << std::endl;
        }
    }

#ifdef ENABLE_FST
    fst_dump_once(tfp, sim_time);
#endif

#ifdef ENABLE_DIFFTEST
    if (difftest != nullptr)
    {
        if (dpic_difftest_step_triggered)
        {
            dpic_difftest_step_triggered = false;
            if (!difftest->step())
            {
#ifdef ENABLE_FST
                tfp->close();
#endif
                return false;
            }
        }
    }
    else
    {
        printf("Difftest failed to initialize!\n");
        exit(1);
    }
#endif

    return true;
}

void dpic_ebreak()
{
    dpic_ebreak_triggered = true;
}
void dpic_difftest_step()
{
    dpic_difftest_step_triggered = true;
}