#include "include/mem.h"

#define MEM_SIZE (1024 * 1024)
#define BEGIN_ADDR 0x80000000
uint8_t memory[MEM_SIZE];
uint32_t prev_mem_addr = 0;


static uint32_t pmem_to_index(int addr)
{
    uint32_t u_addr = (uint32_t)addr;
    u_addr &= ~0x3;
    prev_mem_addr = u_addr;//调试用
    u_addr -= BEGIN_ADDR;
    if ((u_addr + 3) >= MEM_SIZE)
    {
        return 0;
    }
    return u_addr;
}
int mem_read(int addr)
{
    uint32_t idx = pmem_to_index(addr);
    return memory[idx] | (memory[idx + 1] << 8) | (memory[idx + 2] << 16) | (memory[idx + 3] << 24);
}

void mem_write(int addr, int data, char wmask)
{
    uint32_t idx = pmem_to_index(addr);
    memory[idx] = (wmask & 0x1) ? (data & 0xFF) : memory[idx];
    memory[idx + 1] = (wmask & 0x2) ? ((data >> 8) & 0xFF) : memory[idx + 1];
    memory[idx + 2] = (wmask & 0x4) ? ((data >> 16) & 0xFF) : memory[idx + 2];
    memory[idx + 3] = (wmask & 0x8) ? ((data >> 24) & 0xFF) : memory[idx + 3];
}

int mem_print(int addr = BEGIN_ADDR, int len = 64)
{
    uint32_t u_addr = (uint32_t)addr;
    u_addr &= ~0x3;
    if ((u_addr + len - 1) >= MEM_SIZE)
    {
        std::cerr << "Memory read out of bounds: " << std::hex << u_addr << std::dec << std::endl;
        return 0;
    }
    std::cout << "Memory dump from " << std::hex << u_addr << " to " << (u_addr + len - 1) << ":" << std::dec;

    u_addr -= BEGIN_ADDR; // 转换为 memory 数组的索引
    for (int i = 0; i < len; i += 4)
    {
        if (i % 16 == 0)
        {
            std::cout << std::endl
                      << std::hex << (u_addr + i) << ": ";
        }
        // 打印4字节，从高地址到低地址
        for (int j = 3; j >= 0; --j)
        {
            printf("%02x", memory[u_addr + i + j]);
        }
        printf(" ");
    }
    std::cout << std::dec << std::endl;
    return 0;
}

void load_binary(const std::string &filename)
{
    std::ifstream file(filename, std::ios::binary);
    if (!file)
    {
        std::cerr << "Error: Could not open file " << filename << std::endl;

        exit(1);
    }

    // 获取文件大小
    file.seekg(0, std::ios::end);
    std::streamsize size = file.tellg();
    file.seekg(0, std::ios::beg);

    if (size > MEM_SIZE)
    {
        std::cerr << "Error: File size (" << size << ") is larger than memory size (" << MEM_SIZE << ")" << std::endl;
        exit(1);
    }

    // 读取文件内容直接到 memory 数组
    // memory 是 uint8_t 指针，正好对应 char 读取
    if (file.read((char *)memory, size))
    {
        std::cout << "Loaded " << size << " bytes from " << filename << " to memory." << std::endl;
    }
    else
    {
        std::cerr << "Error: Failed to read file content." << std::endl;
        exit(1);
    }
}