#pragma once
#include "common.h"
#include <stdbool.h>
#include <stdint.h>

// Matches NEMU riscv32 CPU_state layout (riscv32, non-RVE, non-RV64)
// gpr[32] + pc  =>  33 * sizeof(uint32_t) = 132 bytes
#define NPC_REF_GPR_NUM 32
typedef struct {
  uint32_t gpr[NPC_REF_GPR_NUM];
  uint32_t pc;
} riscv32_CPU_state;

// RV32E: NPC only has 16 general-purpose registers
#define NPC_GPR_NUM 16

enum { DIFFTEST_TO_DUT = 0, DIFFTEST_TO_REF = 1 };

// Initialize difftest: load NEMU .so and sync memory/PC state
void difftest_init(const char *ref_so_file, uint32_t reset_pc);

// Called after each NPC instruction to compare DUT vs REF
// npc_regs: pointer to array of 16 GPR values; npc_pc: program counter after instruction
void difftest_step(uint32_t *npc_regs, uint32_t npc_pc);

extern bool difftest_enabled;
extern std::string g_diff_so_path;
