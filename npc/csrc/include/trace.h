#pragma once

#include "include/common.h"


void itrace_write(word_t pc,word_t inst);
void itrace_init(std::string build_dir);

void mtrace_write_r(word_t addr, word_t data);
void mtrace_write_w( word_t addr, word_t data, char wmask);

void trace_log();