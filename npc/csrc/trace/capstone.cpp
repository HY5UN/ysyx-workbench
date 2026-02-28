
#include <dlfcn.h>
#include <capstone/capstone.h>
#include <assert.h>
#include <stdio.h>
#include <stdint.h>

#if defined(__APPLE__)
#define CS_LIB_SUFFIX "5.dylib"
#elif defined(__linux__)
#define CS_LIB_SUFFIX "so.5"
#else
#error "Unsupported platform"
#endif

static size_t (*cs_disasm_dl)(csh handle, const uint8_t *code,
    size_t code_size, uint64_t address, size_t count, cs_insn **insn);
static void (*cs_free_dl)(cs_insn *insn, size_t count);

static csh g_cs_handle;

void init_disasm_rv32(void) {
  void *dl_handle = dlopen("tools/capstone/repo/libcapstone." CS_LIB_SUFFIX, RTLD_LAZY);
  assert(dl_handle && "dlopen capstone failed");

  cs_err (*cs_open_dl)(cs_arch arch, cs_mode mode, csh *handle) = NULL;
  cs_open_dl = (cs_err (*)(cs_arch, cs_mode, csh *))dlsym(dl_handle, "cs_open");
  assert(cs_open_dl && "dlsym cs_open failed");

  cs_disasm_dl = (size_t (*)(csh, const uint8_t*, size_t, uint64_t, size_t, cs_insn**))dlsym(dl_handle, "cs_disasm");
  assert(cs_disasm_dl && "dlsym cs_disasm failed");

  cs_free_dl = (void (*)(cs_insn*, size_t))dlsym(dl_handle, "cs_free");
  assert(cs_free_dl && "dlsym cs_free failed");

  cs_arch arch = CS_ARCH_RISCV;

  // RV32 + (可选)C扩展：如果你确认你的指令流包含压缩指令，就保留 CS_MODE_RISCVC
  cs_mode mode = (cs_mode)(CS_MODE_RISCV32 | CS_MODE_RISCVC);

  int ret = cs_open_dl(arch, mode, &g_cs_handle);
  assert(ret == CS_ERR_OK && "cs_open failed");
}

void disassemble_rv32(char *out, int out_size, uint32_t pc, const uint8_t *code, int nbyte) {
  cs_insn *insn = NULL;

  // count=1：只反汇编一条
  size_t count = cs_disasm_dl(g_cs_handle, code, (size_t)nbyte, (uint64_t)pc, 1, &insn);
  assert(count == 1 && "cs_disasm did not return exactly 1 instruction");

  int ret = snprintf(out, out_size, "%s", insn[0].mnemonic);
  if (insn[0].op_str[0] != '\0') {
    snprintf(out + ret, (size_t)(out_size - ret), "\t%s", insn[0].op_str);
  }

  cs_free_dl(insn, count);
}