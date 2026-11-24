/***************************************************************************************
 * Copyright (c) 2014-2024 Zihao Yu, Nanjing University
 *
 * NEMU is licensed under Mulan PSL v2.
 * You can use this software according to the terms and conditions of the Mulan PSL v2.
 * You may obtain a copy of Mulan PSL v2 at:
 *          http://license.coscl.org.cn/MulanPSL2
 *
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND,
 * EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT,
 * MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 *
 * See the Mulan PSL v2 for more details.
 ***************************************************************************************/

#include <isa.h>
#define IRQ_TIMER 0x80000007

#define MIE 0x00000008
#define MPIE 0x00000080
#define MSTATUS cpu.csr[1]

word_t isa_raise_intr(word_t NO, vaddr_t epc)
{
  /* TODO: Trigger an interrupt/exception with ``NO''.
   * Then return the address of the interrupt/exception vector.
   */



  word_t exception(word_t pc, word_t mcause);
  return exception(epc, NO);
}

word_t isa_query_intr()
{
  // printf("isa_query_intr: %x, mstatus: %x\n", cpu.INTR, MSTATUS );
  if (cpu.INTR && ((MSTATUS & MIE) == MIE))
  {
    // printf("isa_query_intr: %x\n", IRQ_TIMER);
    cpu.INTR = false;
    return IRQ_TIMER;
  }
  return INTR_EMPTY;
}
