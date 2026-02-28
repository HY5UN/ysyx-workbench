#pragma once

#include "include/common.h"


void itrace_write(word_t pc,word_t inst);
void itrace_init(std::string build_dir);