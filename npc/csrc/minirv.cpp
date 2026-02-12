#include "Vtop.h"
#include "verilated.h"
#include <iostream>
#include <vector>
#include <cstdint>

#define MEM_SIZE (64 * 1024 * 1024) // 64 MB

class CorrectSimulator
{
public:
    uint32_t PC = 0;
    uint32_t REG[32] = {0};
    uint32_t *RAM;

    CorrectSimulator(void* mem_addr_begin)
    {
        RAM = (uint32_t*)mem_addr_begin;
    }

    bool compare(Vtop* top)
    {
        if (PC != top->io_pc)
        {
            std::cout << "PC mismatch: correct=" << std::hex << PC << " dut=" << top->io_pc << std::dec << std::endl;
            return false;
        }
        uint32_t *addr = (uint32_t *)&top->io_allReg_0;
        for (int i = 0; i < 16; i++)
        {
            if (addr[i] != REG[i])
            {
                std::cout << "Register x" << i << " mismatch: correct=" << std::hex << REG[i] << " dut=" << addr[i] << std::dec << std::endl;
                return false;
            }
            
        }
        return true;
    }

    void inst_cycle()
    {
        uint32_t inst = RAM[PC >> 2];
        uint32_t opcode = inst & 0x7F;
        uint32_t funct3 = (inst >> 12) & 0x07;
        uint32_t funct7 = (inst >> 25) & 0x7F;

        uint32_t rd = (inst >> 7) & 0x1F;
        uint32_t rs1 = (inst >> 15) & 0x1F;
        uint32_t rs2 = (inst >> 20) & 0x1F;

        uint32_t next_PC = PC + 4;

        if (opcode == 0b0110011) // add
        {
            if (rd != 0)
            {
                REG[rd] = REG[rs1] + REG[rs2];
            }
        }

        else if (opcode == 0b0010011) // addi
        {
            int32_t imm = (int32_t)inst >> 20;
            if (rd != 0)
            {
                REG[rd] = REG[rs1] + imm;
            }
        }

        else if (opcode == 0b0110111) // lui
        {
            if (rd != 0)
            {
                REG[rd] = inst & 0xFFFFF000;
            }
        }

        else if (opcode == 0b0000011)
        {
            int32_t imm = (int32_t)inst >> 20;
            uint32_t addr = REG[rs1] + imm;

            uint32_t word_index = addr >> 2;
            uint32_t byte_offset = (addr & 0x3) * 8; // 0, 8, 16, 24

            if (funct3 == 0b010) // lw
            {
                if (rd != 0)
                {
                    REG[rd] = RAM[word_index];
                }
            }

            else if (funct3 == 0b100) // lbu
            {
                if (rd != 0)
                {
                    uint32_t word = RAM[word_index];
                    REG[rd] = (word >> byte_offset) & 0xFF;
                }
            }
        }

        else if (opcode == 0b0100011)
        {
            int32_t imm = ((int32_t)(inst & 0xFE000000) >> 20) | ((inst >> 7) & 0x1F);
            uint32_t addr = REG[rs1] + imm;
            uint32_t word_index = addr >> 2;
            uint32_t byte_offset = (addr & 0x3) * 8;

            if (funct3 == 0b010) // sw
            {
                RAM[word_index] = REG[rs2];
            }

            else if (funct3 == 0b000) // sb
            {
                uint32_t mask = 0xFF << byte_offset;
                uint32_t current_word = RAM[word_index];
                uint32_t byte_to_write = (REG[rs2] & 0xFF) << byte_offset;

                RAM[word_index] = (current_word & ~mask) | byte_to_write;
            }
        }

        else if (opcode == 0b1100111) // jalr
        {
            int32_t imm = (int32_t)inst >> 20;
            uint32_t target = (REG[rs1] + imm) & ~1;

            if (rd != 0)
            {
                REG[rd] = PC + 4;
            }
            next_PC = target;
        }

        else if (opcode == 0b1110011) // ebreak
        {
            if (REG[10] == 0)
            {
                printf("HIT GOOD TRAP\n");
            }
            else
            {
                printf("HIT BAD TRAP\n");
            }
            exit(0);
        }

        else
        {
            printf("incorrect instruction code: 0x%08x\n", inst);
        }
        PC = next_PC;
        REG[0] = 0;
    }

    
};