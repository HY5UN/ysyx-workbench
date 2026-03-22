
#include <cpu/cpu.h>
#include <cpu/decode.h>
#define MEPC 0x341
#define MSTATUS 0x300
#define MCAUSE 0x342
#define MTVEC 0x305

void ecall(Decode *s,word_t mcause) {
    cpu.csr[MEPC]=s->pc;
    cpu.csr[MCAUSE]=mcause;
    s->dnpc=cpu.csr[MTVEC];
    //printf("ETRACE: exception at pc = " FMT_WORD ", dnpc = " FMT_WORD ", mcause = " FMT_WORD "\n", s->pc, s->dnpc, mcause);
}