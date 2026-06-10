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
#include <cpu/cpu.h>
#include <difftest-def.h>
#include <memory/paddr.h>

__EXPORT void difftest_memcpy(paddr_t addr, void *buf, size_t n, bool direction)
{
  printf("difftest_memcpy: addr = 0x%08x, buf = %p, n = %zu, direction = %s\n",
         addr, buf, n, direction == DIFFTEST_TO_REF ? "to_ref" : "to_dut");
  if (direction == DIFFTEST_TO_REF)
  {
    for (size_t i = 0; i < n; i++)
    {
      paddr_write(addr + i, 1, ((uint8_t *)buf)[i]);
    }
  }
  else
  {
    for (size_t i = 0; i < n; i++)
    {
      ((uint8_t *)buf)[i] = paddr_read(addr + i, 1);
    }
  }
}

typedef struct
{
  word_t gpr[32];
  vaddr_t pc;
} dut_cpu_state;

dut_cpu_state *dut_cpu_ptr = NULL;

__EXPORT void difftest_regcpy(void *dut, bool direction)
{

  if (direction == DIFFTEST_TO_DUT)
  {
    memcpy(dut, &cpu, sizeof(dut_cpu_state));
  }
  else
  {
    memcpy(&cpu, dut, sizeof(dut_cpu_state));
    // printf("difftest_regcpy: cpu.pc = 0x%08x\n", cpu.pc);
  }
}

bool difftest_skip_once = false;

__EXPORT void difftest_exec(uint64_t n)
{
  cpu_exec(n);
  printf("difftest_exec: executed %lu instructions\n", (unsigned long)n);
  if (difftest_skip_once)
  {
    difftest_skip_once = false;
    difftest_regcpy(dut_cpu_ptr, DIFFTEST_TO_REF);
  }
}

__EXPORT void difftest_raise_intr(word_t NO)
{
  assert(0);
}

__EXPORT void difftest_init(void *dut)
{
  dut_cpu_ptr = (dut_cpu_state *)dut;

#ifdef CONFIG_DIFFTEST_REF_FOR_YSYXSOC
  cpu.pc = 0x20000000;
  return;
#endif

  void init_mem();
  init_mem();

  /* Perform ISA dependent initialization. */
  init_isa();
}


// ysyxsoc 新增功能
#ifdef CONFIG_DIFFTEST_REF_FOR_YSYXSOC

/* ── Address map ─────────────────────────────────────────────────── */
#define MROM_BASE 0x20000000u
#define MROM_SIZE 0x00001000u /* 4 KB */

#define SRAM_BASE 0x0f000000u
#define SRAM_SIZE 0x00002000u /* 8 KB */

/* ── Backing storage ─────────────────────────────────────────────── */
static uint8_t mrom_mem[MROM_SIZE];
static uint8_t sram_mem[SRAM_SIZE];

/* ── Device descriptor ───────────────────────────────────────────── */
typedef struct
{
  paddr_t base;
  size_t size;
  uint8_t *mem;
  const char *name;
} SoCDevice;

static SoCDevice soc_devices[] = {
    {MROM_BASE, MROM_SIZE, mrom_mem, "MROM"},
    {SRAM_BASE, SRAM_SIZE, sram_mem, "SRAM"},
    /* 新增设备：在这里追加一行即可 */
};

#define NR_SOC_DEVICES (sizeof(soc_devices) / sizeof(soc_devices[0]))

/* ── Lookup ──────────────────────────────────────────────────────── */
static SoCDevice *soc_find_device(paddr_t addr)
{
  for (size_t i = 0; i < NR_SOC_DEVICES; i++)
  {
    if (addr >= soc_devices[i].base &&
        addr < soc_devices[i].base + soc_devices[i].size)
      return &soc_devices[i];
  }
  return NULL;
}


/* ── Public API ──────────────────────────────────────────────────── */
word_t soc_addr_read(paddr_t addr, int len)
{
  SoCDevice *dev = soc_find_device(addr);
  if (dev == NULL)
  {
    difftest_skip_once = true;
    return 0;
  }

  uint32_t offset = addr - dev->base;
  word_t data = 0;
  for (int i = 0; i < len; i++)
    data |= (word_t)(dev->mem[offset + i]) << (i * 8);
  return data;
}

void soc_addr_write(paddr_t addr, int len, word_t data)
{
  SoCDevice *dev = soc_find_device(addr);
  if (dev == NULL)
  {
    difftest_skip_once = true;
    return;
  }

  uint32_t offset = addr - dev->base;
  for (int i = 0; i < len; i++)
    dev->mem[offset + i] = (data >> (i * 8)) & 0xff;
}

#endif /* CONFIG_DIFFTEST_REF_FOR_YSYXSOC */