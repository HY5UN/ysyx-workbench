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
#include <memory/vaddr.h>
#include <memory/paddr.h>

#define SATP cpu.csr[4]

int isa_mmu_check(vaddr_t vaddr, int len, int type)
{
  if (SATP & 0x80000000)
  {
    // printf("satp: %x\n", SATP);
    return MMU_TRANSLATE;
  }
  else
  {
    return MMU_DIRECT;
  }
}

paddr_t isa_mmu_translate(vaddr_t vaddr, int len, int type)
{
  word_t pd_idx = vaddr >> 22;
  word_t pt_idx = (vaddr >> 12) & 0x3FF;
  paddr_t pd = SATP << 12;
  paddr_t pt = paddr_read(pd + pd_idx * 4, 4) >> 10 << 12;
  paddr_t pg = paddr_read(pt + pt_idx * 4, 4) >> 10 << 12;

  paddr_t paddr = pg  | (vaddr & 0xFFF);
  if(vaddr >=0x80000000){
    Assert(paddr == vaddr,"Kernal addr space should be identity mapping\n");
  }
  // if(vaddr == 0x40054f18){
  //   printf("vaddr: 0x%08x, paddr: 0x%08x, ", vaddr, paddr);
  //   printf("SATP: 0x%08x, pd: 0x%08x, pt: 0x%08x, pg: 0x%08x, ", SATP, pd, pt, pg);
  //   printf("pg entry paddr: 0x%08x\n",pt + pt_idx * 4);
  // }
  return paddr;
}
