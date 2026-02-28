#include "include/common.h"

// void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);

static char logbuf[256];
static int buf_pos = 0;
static std::string itrace_log_file = "";

void itrace_write(word_t pc, word_t inst)
{
    char asmstr[128];

    // 把 32-bit inst 拆成小端字节序（RISC-V 指令在内存里是 little-endian）
    uint8_t code[4];
    code[0] = (uint8_t)(inst & 0xff);
    code[1] = (uint8_t)((inst >> 8) & 0xff);
    code[2] = (uint8_t)((inst >> 16) & 0xff);
    code[3] = (uint8_t)((inst >> 24) & 0xff);

    // 传 4 字节最稳：就算是 16-bit 压缩指令也够 capstone 判断
    disassemble_rv32(asmstr, sizeof(asmstr), (uint32_t)pc, code, 4);

    buf_pos += sprintf(logbuf + buf_pos, "[pc]0x%08x: %s", (uint32_t)pc, asmstr);
}
void itrace_init(std::string build_dir)
{

    itrace_log_file = build_dir + "/itrace-log.txt";
    FILE *fp = fopen(itrace_log_file.c_str(), "w");
    if (fp != NULL)
    {
        fclose(fp);
    }

    init_disasm_rv32();
}

static int mem_acess_count = 0;
void mtrace_write_r(word_t addr, word_t data)
{
    if(mem_acess_count++>=1){
        return;
    }
    buf_pos += sprintf(logbuf + buf_pos, " [R addr=0x%08x: 0x%08x]", addr, data);
}
void mtrace_write_w(word_t addr, word_t data, char wmask)
{
    if(mem_acess_count++>=1){
        return;
    }
    buf_pos += sprintf(logbuf + buf_pos, " [W addr=0x%08x: 0x%08x wmask=0b%04b]", addr, data, wmask);
}

static inline void trace_reset()
{
    mem_acess_count = 0;
    buf_pos = 0;
    logbuf[0] = '\0';
}

static int log_count = 0;
void trace_log()
{
    if (log_count++ > 10000)
    {
        trace_reset();
        return;
    }
    FILE *fp = std::fopen(itrace_log_file.c_str(), "a");
    if (!fp)
        return;

    std::fwrite(logbuf, 1, (size_t)buf_pos, fp);
    std::fputc('\n', fp);
    std::fclose(fp);

    trace_reset();
}