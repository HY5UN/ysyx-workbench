
#include <cpu/cpu.h>
#include <cpu/decode.h>
#include "local-include/reg.h"

#define MIE 0x00000008
#define MPIE 0x00000080
word_t exception(word_t pc, word_t mcause)
{
    if ((csr(MSTATUS) & MIE) == MIE)
    {
        csr(MSTATUS) |= MPIE;
    }
    else
    {
        csr(MSTATUS) &= ~MPIE;
    }
    csr(MSTATUS) &= ~MIE;
    csr(MEPC) = pc;
    csr(MCAUSE) = mcause;
    return csr(MTVEC);
    // printf("ETRACE: exception at pc = " FMT_WORD ", dnpc = " FMT_WORD ", mcause = " FMT_WORD "\n", s->pc, s->dnpc, mcause);
}


word_t mret()
{
    if ((csr(MSTATUS) & MPIE) == MPIE)
    {
        csr(MSTATUS) |= MIE;
    }
    else
    {
        csr(MSTATUS) &= ~MIE;
    }
    csr(MSTATUS) |= MPIE;

    return csr(MEPC);
}

uint32_t default_csr = 0;