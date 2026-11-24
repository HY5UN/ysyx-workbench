#define concat_temp(x, y) x ## y
#define concat(x, y) concat_temp(x, y)
#define MAP(c, f) c(f)

#if __riscv_xlen == 32
#define LOAD  lw
#define STORE sw
#define XLEN  4
#else
#define LOAD  ld
#define STORE sd
#define XLEN  8
#endif

#define REGS_LO16(f) \
      f( 1)       f( 3) f( 4) f( 5) f( 6) f( 7) f( 8) f( 9) \
f(10) f(11) f(12) f(13) f(14) f(15)
#ifndef __riscv_e
#define REGS_HI16(f) \
                                    f(16) f(17) f(18) f(19) \
f(20) f(21) f(22) f(23) f(24) f(25) f(26) f(27) f(28) f(29) \
f(30) f(31)
#define NR_REGS 32
#else
#define REGS_HI16(f)
#define NR_REGS 16
#endif

#define REGS(f) REGS_LO16(f) REGS_HI16(f)

#define PUSH(n) STORE concat(x, n), (n * XLEN)(sp);
#define POP(n)  LOAD  concat(x, n), (n * XLEN)(sp);

// must match struct Context in arch/riscv.h:
//   gpr[NR_REGS], mcause, mstatus, mepc, pdir, np
#define CONTEXT_SIZE  ((NR_REGS + 5) * XLEN)
#define OFFSET_SP     ( 2 * XLEN)            // c->sp  -> c->gpr[sp]
#define OFFSET_T0     ( 5 * XLEN)            // c->gpr[t0]
#define OFFSET_CAUSE  ((NR_REGS + 0) * XLEN)
#define OFFSET_STATUS ((NR_REGS + 1) * XLEN)
#define OFFSET_EPC    ((NR_REGS + 2) * XLEN)
#define OFFSET_PDIR   ((NR_REGS + 3) * XLEN)
#define OFFSET_NP     ((NR_REGS + 4) * XLEN)

// c->np
#define KERNEL 0
#define USER   1


.align 3
.globl __am_asm_trap
__am_asm_trap:
  csrrw sp, mscratch, sp   // (1) atomically exchange sp and mscratch
  bnez sp, from_user       // (2) take the branch if we trapped from user
  csrr sp, mscratch        // (3) if we trapped from kernel, restore the original sp

  addi sp, sp, -CONTEXT_SIZE
  STORE t0, OFFSET_T0(sp)  // free t0 to record np
  li t0, KERNEL
  j save_context

from_user:
  // from user: sp is now ksp (bottom of kernel stack), np = USER
  addi sp, sp, -CONTEXT_SIZE
  STORE t0, OFFSET_T0(sp)
  li t0, USER

save_context:
  STORE t0, OFFSET_NP(sp)  // c->np = pp
  LOAD t0, OFFSET_T0(sp)   // recover t0, it is saved again below

  MAP(REGS, PUSH)

  // in both paths, mscratch now holds $sp before trapping
  csrr t0, mscratch
  STORE t0, OFFSET_SP(sp)  // c->sp = $sp
  csrw mscratch, zero      // ksp = 0: we are in kernel now (support re-entry of CTE)

  csrr t0, mcause
  csrr t1, mstatus
  csrr t2, mepc

  STORE t0, OFFSET_CAUSE(sp)
  STORE t1, OFFSET_STATUS(sp)
  STORE t2, OFFSET_EPC(sp)

  # set mstatus.MPRV to pass difftest
  li a0, (1 << 17)
  or t1, t1, a0
  csrw mstatus, t1

  mv a0, sp
  call __am_irq_handle

  mv sp, a0 # 将 sp 切换为返回的新 Context 地址

  LOAD t1, OFFSET_STATUS(sp)
  LOAD t2, OFFSET_EPC(sp)
  csrw mstatus, t1
  csrw mepc, t2

  // if (c->np == USER) ksp = $sp after popping the context (bottom of kernel stack)
  LOAD t0, OFFSET_NP(sp)
  beqz t0, restore_regs
  addi t0, sp, CONTEXT_SIZE
  csrw mscratch, t0

restore_regs:
  MAP(REGS, POP)

  LOAD sp, OFFSET_SP(sp)   // $sp = c->sp (user stack, or kernel sp before trapping)
  mret
