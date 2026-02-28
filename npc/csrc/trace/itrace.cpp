#include "include/common.h"

// void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);

static char logbuf[256];
static int buf_pos = 0;
static std::string itrace_log_file = "";

void itrace_write(word_t pc, word_t inst)
{
    buf_pos += sprintf(logbuf + buf_pos, "[pc]0x%08x: 0x%08x", pc, inst);
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

static int mem_acess_count = 0;
void mtrace_write_r(word_t addr, word_t data)
{
    if(mem_acess_count++>1){
        return;
    }
    buf_pos += sprintf(logbuf + buf_pos, " [R addr=0x%08x: 0x%08x]", addr, data);
}
void mtrace_write_w(word_t addr, word_t data, char wmask)
{
    if(mem_acess_count++>1){
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