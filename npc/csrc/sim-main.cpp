#include "Vysyx_26010036.h"
#include "verilated.h"
#include <iostream>
#include <vector>
#include <cstdint>
#include <fstream>
#include <Vysyx_26010036__Dpi.h>
#include "include/common.h"
#include "include/mem.h"
#include "include/DeviceIO.h"
#include "include/trace.h"
#include "include/config.h"

static bool starts_with(const std::string &s, const std::string &prefix)
{
    return s.size() >= prefix.size() && s.compare(0, prefix.size(), prefix) == 0;
}
static std::string after_prefix(const std::string &s, const std::string &prefix)
{
    return starts_with(s, prefix) ? s.substr(prefix.size()) : std::string();
}
void parse_args(int argc, char **argv)
{
    for (int i = 1; i < argc; ++i)
    {
        std::string arg = argv[i] ? argv[i] : "";

        if (starts_with(arg, "IMG="))
        {
            std::string img_path = after_prefix(arg, "IMG=");
            std::cout << "Loading binary from: " << img_path << std::endl;
            if(load_binary(img_path))
            {
                std::cout << "Binary loaded successfully." << std::endl;
            }
            else
            {
                std::cerr << "Failed to load binary, loading default binary instead." << std::endl;
                img_path = "resource/alutest-minirv-npc.bin";
                if(!load_binary(img_path))
                {
                    std::cerr << "Failed to load default binary." << std::endl;
                    exit(1);
                }
            }
            #ifdef ENABLE_FTRACE
            if (!init_ftrace(img_path.c_str()))
            {
                ftrace_enabled = false;
                std::cerr << "Failed to initialize ftrace with binary: " << img_path << std::endl;
            }
            #endif

        }
        else if (starts_with(arg, "BUILD="))
        {
            std::string build_dir = after_prefix(arg, "BUILD=");
            #ifdef ENABLE_ITRACE
            itrace_log_init(build_dir);
            #endif
            #ifdef ENABLE_FTRACE
            ftrace_log_init(build_dir);
            #endif
        }
        
    }
}

int main(int argc, char **argv)
{
    // 打印参数用于调试
    // std::cout << "Arguments: ";
    // for (int i = 0; i < argc; i++)
    // {
    //     std::cout << argv[i] << " ";
    // }
    // std::cout << std::endl;

    parse_args(argc, argv);

    init_devices();

    sdb_mainloop(argc, argv);

    return 0;
}
