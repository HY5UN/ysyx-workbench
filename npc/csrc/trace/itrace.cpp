#include "include/common.h"

// void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);

static char itrace_buf[256];
static int itrace_buf_pos = 0;
static std::string itrace_log_file = "";

void itrace_write(word_t pc, word_t inst)
{
    itrace_buf_pos += sprintf(itrace_buf + itrace_buf_pos, "[pc]0x%08x: 0x%08x", pc, inst);
}
void itrace_init(std::string build_dir)
{

    itrace_log_file = build_dir + "/itrace-log.txt";
    FILE *fp = fopen(itrace_log_file.c_str(), "w");
    if (fp != NULL)
    {
        fclose(fp);
    }
}

static char mtrace_buf[256];
static int mtrace_buf_pos = 0;
void mtrace_write_r(word_t addr, word_t data)
{
    mtrace_buf_pos += sprintf(mtrace_buf + mtrace_buf_pos, " [R addr=0x%08x: 0x%08x]", addr, data);
}
void mtrace_write_w(word_t addr, word_t data, char wmask)
{
    mtrace_buf_pos += sprintf(mtrace_buf + mtrace_buf_pos, " [W addr=0x%08x: 0x%08x wmask=0b%04b]", addr, data, wmask);
}

static inline void trace_reset()
{
    itrace_buf_pos = 0;
    mtrace_buf_pos = 0;
    itrace_buf[0] = '\0';
    mtrace_buf[0] = '\0';
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

    std::fwrite(itrace_buf, 1, (size_t)itrace_buf_pos, fp);
    std::fwrite(mtrace_buf, 1, (size_t)mtrace_buf_pos, fp);
    std::fputc('\n', fp);
    std::fclose(fp);

    trace_reset();
}