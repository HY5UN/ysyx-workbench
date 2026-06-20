#include "include/common.h"

static char itrace_buf[512];
static char mtrace_buf[512];
static int itrace_buf_pos = 0;
static int mtrace_buf_pos= 0;
static std::string itrace_log_file = "";

// struct Mtrace_r
// {
//     word_t addr;
//     word_t data;
// } mtrace_r;
// struct Mtrace_w
// {
//     word_t addr;
//     word_t data;
//     char wmask;
// } mtrace_w;

void itrace_write(word_t pc, word_t inst)
{
    itrace_buf_pos += sprintf(itrace_buf + itrace_buf_pos, "[pc]0x%08x: 0x%08x", pc, inst);
    // if (mtrace_r.addr != 0)
    // {
    //     itrace_buf_pos += sprintf(itrace_buf + itrace_buf_pos, " [R addr=0x%08x: 0x%08x]", mtrace_r.addr, mtrace_r.data);
    //     mtrace_r.addr = 0;
    // }
    // if (mtrace_w.addr != 0)
    // {
    //     itrace_buf_pos += sprintf(itrace_buf + itrace_buf_pos, " [W addr=0x%08x: 0x%08x wmask=0b%04b]", mtrace_w.addr, mtrace_w.data, mtrace_w.wmask);
    //     mtrace_w.addr = 0;
    // }
    itrace_buf_pos += sprintf(itrace_buf + itrace_buf_pos, " %s", mtrace_buf);
}
void itrace_log_init(std::string build_dir)
{

    itrace_log_file = build_dir + "/itrace-log.txt";
    FILE *fp = fopen(itrace_log_file.c_str(), "w");
    if (fp != NULL)
    {
        fclose(fp);
    }
}


void mtrace_record_r(const char *msg)
{
    mtrace_buf_pos += sprintf(mtrace_buf + mtrace_buf_pos, " %s", msg);
    // mtrace_buf_pos += sprintf(mtrace_buf  + mtrace_buf_pos, " [R addr=0x%08x: 0x%08x]", addr ,data);
    // mtrace_r.addr = addr;
    // mtrace_r.data = data;

}
void mtrace_record_w(const char *msg)
{
    mtrace_buf_pos += sprintf(mtrace_buf + mtrace_buf_pos, " %s", msg);
    // mtrace_w.addr = addr;
    // mtrace_w.data = data;
    // mtrace_w.wmask = wmask;
    // mtrace_buf_pos += sprintf(mtrace_buf  + mtrace_buf_pos, " [W addr=0x%08x: 0x%08x wmask=0b%04b]", addr ,data,wmask);

}

void 

static void itrace_reset()
{
    itrace_buf_pos = 0;
    mtrace_buf_pos=0;
    itrace_buf[0] = '\0';
    mtrace_buf[0]= '\0';
}

static int log_count = 0;
void trace_log()
{
    if (log_count++ > 10000)
    {
        FILE *fp = fopen(itrace_log_file.c_str(), "w");
        if (fp != NULL)
        {
            fclose(fp);
        }
        log_count = 0;
    }
    FILE *fp = std::fopen(itrace_log_file.c_str(), "a");
    if (!fp)
        return;

    std::fwrite(itrace_buf, 1, (size_t)itrace_buf_pos, fp);
    std::fputc('\n', fp);
    std::fclose(fp);

    itrace_reset();
}