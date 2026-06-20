#include "include/common.h"
#include "include/trace.h"
#include "include/CPU.h"
#include "include/config.h"

static bool dpic_ebreak_triggered = false;
static bool dpic_inst_finish_flag = false;

CPU::CPU(int argc, char **argv)
{
    contextp = new VerilatedContext;
    contextp->commandArgs(argc, argv);
    top = new VysyxSoCFull{contextp};

#ifdef ENABLE_DIFFTEST
    difftest = new DiffTest();
    if (difftest == nullptr)
    {
        std::cerr << "Failed to initialize DiffTest." << std::endl;
        exit(1);
    }
#endif
    fst_init(top);
}

CPU::~CPU()
{

    fst_close();
    delete top;
    delete contextp;
}

const char *reg_names[] = {
    "$0", "ra", "sp", "gp", "tp", "t0", "t1", "t2",
    "s0", "s1", "a0", "a1", "a2", "a3", "a4", "a5"};

void CPU::reg_print()
{
    for (int i = 0; i < REG_NUM; i++)
    {
        if (i % 8 == 0 && i != 0)
            printf("\n");
        printf("\tx%-2d(%s): 0x%08x ", i, reg_names[i], dut_CPU_state.gpr[i]);
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
        // #ifdef ENABLE_FST
        //         fst_dump_once();
        // #endif
        top->clock = 1;
        top->eval();
        cycle_count++;
#ifdef ENABLE_FST
        fst_dump_once();
#endif
    }
    top->reset = 0;
    top->clock = 0;
    top->eval();

#ifdef ENABLE_FTRACE
    if (ftrace_enabled)
    {
        get_init_func_symbols(pc);
    }

#endif
}

bool CPU::execute(uint64_t steps)
{
    for (; steps > 0 && !contextp->gotFinish() && !dpic_ebreak_triggered; steps--)
    {
        if (!execute_once())
        {
            printf("CPU execution failed at PC = 0x%08x\n", dut_CPU_state.pc);
            return false;
        }
    }
    return true;
}

bool CPU::execute_once()
{

    top->clock = 0;
    top->eval();
#ifdef ENABLE_FST
    fst_dump_once();
#endif
    top->clock = 1;
    top->eval();
    cycle_count++;
#ifdef ENABLE_FST
    fst_dump_once();
#endif

    contextp->timeInc(1);

    if (dpic_ebreak_triggered)
    {
        
#ifdef ENABLE_DIFFTEST
        difftest->in_mismatch = false;
#endif
        printf(">>> 执行 ebreak 指令，仿真结束。pc= 0x%08x  总周期=%llu  总指令=%llu\n", dut_CPU_state.pc, cycle_count, inst_count);
        fst_close();
        if (dut_CPU_state.gpr[10] == 0)
        {
            printf("HIT GOOD TRAP!\n");
        }
        else
        {
            reg_print();
            printf("HIT BAD TRAP! x10 = 0x%08x\n", dut_CPU_state.gpr[10]);
        }
    }
    if (dpic_inst_finish_flag)
    {
        inst_count++;
        dpic_inst_finish_flag = false;

#ifdef ENABLE_ITRACE
        itrace_write(pc, inst);
        trace_log();

#endif

#ifdef ENABLE_FTRACE

        if (ftrace_enabled)
        {
            int rd = (inst >> 7) & 0x1F;
            int rs1 = (inst >> 15) & 0x1F;
            if (was_jal())
                ftrace_record(pc, true);

            else if (was_jalr())
                ftrace_record(pc, false);

            save_prev_state(pc, inst, rd, rs1);
        }

#endif

#ifdef ENABLE_DIFFTEST
        if (difftest->in_mismatch)
        {
            if (difftest->steps_after_mismatch-- > 0)
            {
                return true;
            }
            fst_close();
            return false;
        }
        if (!difftest->step())
        {
            difftest->in_mismatch = true;
            if (difftest->steps_after_mismatch > 0)
            {
                return true;
            }
            fst_close();
            return false;
        }
#endif
    }

    return true;
}

void dpic_ebreak()
{
    dpic_ebreak_triggered = true;
}
void dpic_inst_finish()
{
    dpic_inst_finish_flag = true;
}
