#include "include/common.h"
#include "include/trace.h"
#include "include/CPU.h"
#include "include/mem.h"

static bool ebreak_triggered = false;

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
}

CPU::~CPU()
{
    delete top;
    delete contextp;
}

void CPU::reg_print()
{
    // 打印寄存器 每行8个寄存器
    uint32_t *addr = (uint32_t *)&top->io_allReg_0;
    for (int i = 0; i < REG_NUM; i++)
    {
        if (i % 8 == 0 && i != 0)
            printf("\n");
        printf("\tx%-2d: 0x%08x ", i, addr[i]);
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

void CPU::execute(uint64_t steps)
{
    for (; steps > 0 && !contextp->gotFinish() && !ebreak_triggered; steps--)
    {
        execute_once();
    }
}

void CPU::execute_once()
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
            ftrace_record(top->io_pc, rd, rs1, true);
        }
        else if (was_jalr())
        {
            ftrace_record(top->io_pc, rd, rs1, false);
        }

        save_prev_state(top->io_pc, top->io_inst, rd, rs1);
    }

#endif

    top->clock = 0;
    top->eval();
    top->clock = 1;
    top->eval();

    contextp->timeInc(1);

    if (ebreak_triggered)
    {
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

#ifdef ENABLE_DIFFTEST
    if (difftest != nullptr)
    {
        difftest->step();
    }
#endif
}

void ebreak()
{
    std::cout << ">>> 执行 ebreak 指令，触发仿真结束。" << std::endl;
    ebreak_triggered = true;
}