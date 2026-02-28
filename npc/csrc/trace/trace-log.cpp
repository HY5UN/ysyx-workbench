#include"include/common.h"



void trace_log(std::string file,std::string log){
    std::ofstream out(file,std::ios::app);
    out<<log;
    out.close();
}