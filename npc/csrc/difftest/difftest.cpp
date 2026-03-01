/**
 * NPC DiffTest against NEMU (riscv32 shared object).
 *
 * DUT = NPC (RV32E, 16 GPRs)
 * REF = NEMU riscv32 (32 GPRs, upper 16 are unused/zero for E subset)
 *
 * Workflow:
 *   1. difftest_init() – dlopen NEMU .so, copy program image into NEMU memory,
 *                        sync initial register state.
 *   2. difftest_step() – called after every NPC clock cycle that retires an
 *                        instruction; advances NEMU by 1 step and compares.
 */
#include "include/difftest.h"
#include "include/mem.h"
#include "include/common.h"
#include <dlfcn.h>
#include <cstring>
#include <cstdio>
#include <cstdlib>

// -----------------------------------------------------------------------
// Toggle
// -----------------------------------------------------------------------
bool difftest_enabled = false;

// -----------------------------------------------------------------------
// REF API function pointer types (matching NEMU's exported signatures)
// -----------------------------------------------------------------------
typedef void (*ref_difftest_init_t)(int port);
typedef void (*ref_difftest_memcpy_t)(uint32_t addr, void *buf, size_t n, bool direction);
typedef void (*ref_difftest_regcpy_t)(void *dut, bool direction);
typedef void (*ref_difftest_exec_t)(uint64_t n);

static ref_difftest_init_t    ref_init    = nullptr;
static ref_difftest_memcpy_t  ref_memcpy  = nullptr;
static ref_difftest_regcpy_t  ref_regcpy  = nullptr;
static ref_difftest_exec_t    ref_exec    = nullptr;

// -----------------------------------------------------------------------
// Symbol loader helper
// -----------------------------------------------------------------------
static void *load_sym(void *handle, const char *name)
{
    void *sym = dlsym(handle, name);
    if (!sym) {
        fprintf(stderr, "[difftest] dlsym(%s) failed: %s\n", name, dlerror());
        exit(1);
    }
    return sym;
}

// -----------------------------------------------------------------------
// difftest_init
//   ref_so_file : path to NEMU shared object
//   reset_pc    : NPC reset PC (should be 0x80000000)
// -----------------------------------------------------------------------
void difftest_init(const char *ref_so_file, uint32_t reset_pc)
{
    void *handle = dlopen(ref_so_file, RTLD_LAZY | RTLD_GLOBAL);
    if (!handle) {
        fprintf(stderr, "[difftest] dlopen(%s) failed: %s\n", ref_so_file, dlerror());
        exit(1);
    }

    ref_init   = (ref_difftest_init_t)   load_sym(handle, "difftest_init");
    ref_memcpy = (ref_difftest_memcpy_t) load_sym(handle, "difftest_memcpy");
    ref_regcpy = (ref_difftest_regcpy_t) load_sym(handle, "difftest_regcpy");
    ref_exec   = (ref_difftest_exec_t)   load_sym(handle, "difftest_exec");

    // Initialize NEMU (calls init_mem + init_isa internally)
    ref_init(0);

    // Copy the program image from NPC memory into NEMU memory
    size_t img_size = get_loaded_img_size();
    if (img_size == 0) {
        fprintf(stderr, "[difftest] warning: loaded image size is 0 – no binary loaded yet?\n");
    }
    ref_memcpy(get_raw_mem_base(), get_raw_memory(), img_size, DIFFTEST_TO_REF);

    // Sync initial register state: build a full riscv32 CPU state from NPC
    // (NPC is RV32E with 16 regs; upper 16 GPRs are set to 0)
    riscv32_CPU_state state;
    memset(&state, 0, sizeof(state));
    state.pc = reset_pc;
    // x0..x15 are all 0 after NPC reset – nothing else to set
    ref_regcpy(&state, DIFFTEST_TO_REF);

    difftest_enabled = true;
    printf("[difftest] initialized. REF = %s\n", ref_so_file);
}

// -----------------------------------------------------------------------
// difftest_step
//   Called AFTER NPC has retired one instruction.
//   npc_regs : pointer to NPC gpr[0..NPC_GPR_NUM-1] (post-instruction values)
//   npc_pc   : NPC program counter after instruction
// -----------------------------------------------------------------------
void difftest_step(uint32_t *npc_regs, uint32_t npc_pc)
{
    if (!difftest_enabled) return;

    // Advance REF by one step
    ref_exec(1);

    // Retrieve REF state
    riscv32_CPU_state ref_state;
    ref_regcpy(&ref_state, DIFFTEST_TO_DUT);

    // ----------------------------------------------------------------
    // Compare PC
    // ----------------------------------------------------------------
    bool mismatch = false;

    if (ref_state.pc != npc_pc) {
        printf("[difftest] PC mismatch: DUT=0x%08x  REF=0x%08x\n",
               npc_pc, ref_state.pc);
        mismatch = true;
    }

    // ----------------------------------------------------------------
    // Compare GPRs x0 .. x15 (RV32E)
    // ----------------------------------------------------------------
    for (int i = 0; i < NPC_GPR_NUM; i++) {
        if (ref_state.gpr[i] != npc_regs[i]) {
            printf("[difftest] GPR x%d mismatch at pc=0x%08x: DUT=0x%08x  REF=0x%08x\n",
                   i, npc_pc, npc_regs[i], ref_state.gpr[i]);
            mismatch = true;
        }
    }

    if (mismatch) {
        printf("[difftest] *** MISMATCH DETECTED – halting simulation ***\n");
        exit(1);
    }
}
