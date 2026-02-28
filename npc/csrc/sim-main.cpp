#include "Vtop.h"
#include "verilated.h"
#include <iostream>
#include <vector>
#include <cstdint>
#include <fstream>
#include <Vtop__Dpi.h>
#include "include/common.h"
#include "include/mem.h"
#include "include/DeviceIO.h"

int main(int argc, char **argv)
{
    // 打印参数用于调试
    std::cout << "Arguments: ";
    for (int i = 0; i < argc; i++)
    {
        std::cout << argv[i] << " ";
    }
    std::cout << std::endl;

    if (argc > 1)
    {
        load_binary(argv[1]);
    }
    else
    {
        const std::string default_bin = "resource/alutest-minirv-npc.bin";
        std::cout << "No binary file provided. Loading default:" << default_bin << std::endl;
        load_binary(default_bin);
    }

    init_devices();

    sdb_mainloop(argc, argv);

    return 0;
}
