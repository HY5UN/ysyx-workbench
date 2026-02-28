#include"include/common.h"


//void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);

static char logbuf[128];
static std::string itrace_log_file = "";

void itrace_write(word_t pc,word_t inst){
    sprintf(logbuf, "0x%08x: 0x%08x\n", pc, inst);
}
itrace_init(std::string build_dir){
    itrace_log_file = build_dir + "/itrace-log.txt";
}