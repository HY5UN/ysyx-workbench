#include "include/mem.h"
#include "include/DeviceIO.h"
#include "include/trace.h"

#define MEM_SIZE (1024 * 1024 * 64)
#define BEGIN_ADDR 0x80000000
uint8_t memory[MEM_SIZE];
uint32_t prev_mem_addr = 0;

static bool pmem_to_index(int addr, uint32_t &idx)
{
    uint32_t u_addr = (uint32_t)addr;
    u_addr &= ~0x3;
    prev_mem_addr = u_addr; // 调试用
    u_addr -= BEGIN_ADDR;
    if ((u_addr + 3) >= MEM_SIZE)
    {
        return false;
    }
    idx = u_addr;
    return true;
}
int mem_read(int addr)
{
    int data;
    int mmio_data;
    uint32_t idx;

    if (handle_mmio_read(addr, mmio_data))
    {
        data = mmio_data;
    }
    else if (pmem_to_index(addr, idx))
    {
        data = memory[idx] | (memory[idx + 1] << 8) | (memory[idx + 2] << 16) | (memory[idx + 3] << 24);
    }
    else
    {
        data = 0;
    }

#ifdef ENABLE_MTRACE
    if (cpu->top->io_pc != addr)
    {
        mtrace_write_r(addr, data);
    }
#endif

    return data;
}

void mem_write(int addr, int data, char wmask)
{
#ifdef ENABLE_MTRACE
    mtrace_write_w(addr, data, wmask);
#endif

    if (handle_mmio_write(addr, data, wmask))
    {
        return;
    }

    uint32_t idx;
    if (!pmem_to_index(addr, idx))
    {
        return;
    }
    memory[idx] = (wmask & 0x1) ? (data & 0xFF) : memory[idx];
    memory[idx + 1] = (wmask & 0x2) ? ((data >> 8) & 0xFF) : memory[idx + 1];
    memory[idx + 2] = (wmask & 0x4) ? ((data >> 16) & 0xFF) : memory[idx + 2];
    memory[idx + 3] = (wmask & 0x8) ? ((data >> 24) & 0xFF) : memory[idx + 3];
}

int mem_print(uint32_t addr, int len)
{
    uint32_t idx;
    if (!pmem_to_index(addr, idx))
    {
        printf("Mem print out of range: 0x%08x\n", addr);
        return -1;
    }
    for (int i = 0; i < len; i += 4)
    {
        uint32_t data = memory[idx + i] | (memory[idx + i + 1] << 8) | (memory[idx + i + 2] << 16) | (memory[idx + i + 3] << 24);
        printf("0x%08x: 0x%08x\t", addr + i, data);
        if ((i + 4) % 16 == 0)
        {
            printf("\n");
        }
    }
    printf("\n");
    return 0;
}

bool load_binary(const std::string &filename)
{
    std::ifstream file(filename, std::ios::binary);
    if (!file)
    {
        std::cerr << "Error: Could not open file " << filename << std::endl;

        return false;
    }

    // 获取文件大小
    file.seekg(0, std::ios::end);
    std::streamsize size = file.tellg();
    file.seekg(0, std::ios::beg);

    if (size > MEM_SIZE)
    {
        std::cerr << "Error: File size (" << size << ") is larger than memory size (" << MEM_SIZE << ")" << std::endl;
        return false;
    }

    // 读取文件内容直接到 memory 数组
    // memory 是 uint8_t 指针，正好对应 char 读取
    if (file.read((char *)memory, size))
    {
        std::cout << "Loaded " << size << " bytes from " << filename << " to memory." << std::endl;
        return true;
    }
    else
    {
        std::cerr << "Error: Failed to read file content." << std::endl;
        return false;
    }
}