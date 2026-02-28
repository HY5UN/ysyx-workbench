#pragma once

#include "include/common.h"

#define ENABLE_ITRACE

#define ENABLE_FTRACE

void itrace_write(word_t pc, word_t inst);
void itrace_log_init(std::string build_dir);

void mtrace_write_r(word_t addr, word_t data);
void mtrace_write_w(word_t addr, word_t data, char wmask);

void trace_log();

typedef struct
{
    char *name;
    word_t addr_begin;
    word_t addr_end;

} FuncSymbol;

extern bool ftrace_enabled;

bool init_ftrace(const char *bin_path);
void ftrace_log_init(std::string build_dir);
void get_init_func_symbols(int pc);
void ftrace_record(word_t curr_pc, int rd, int rs1, bool is_jal);
void save_prev_state(word_t pc, word_t inst);
bool was_jal();
bool was_jalr();