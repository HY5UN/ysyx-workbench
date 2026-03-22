package top

import chisel3._
import chisel3.util._

import ControlConstants._
import RV32EInstr._

class RV32EDecoder extends Module {
  val io     = IO(new Bundle {
    val inst = Input(UInt(32.W))

    val rs1 = Output(UInt(5.W))
    val rs2 = Output(UInt(5.W))
    val rd  = Output(UInt(5.W))
    val imm = Output(UInt(32.W))

    // val aluOp        = Output(UInt(4.W))
    // val op1Sel       = Output(UInt(1.W))
    // val op2Sel       = Output(UInt(1.W))
    // val rdSel        = Output(UInt(2.W))
    // val regWen       = Output(Bool())
    // val memReadValid = Output(Bool())
    // val memWen       = Output(Bool())
    // val memLen       = Output(UInt(2.W))
    // val memSext      = Output(Bool())
    // val pcSel        = Output(UInt(2.W))
    // val ebreak       = Output(Bool())

    val ctrl = Output(new CtrlBundle)

  })
  val opcode = io.inst(6, 0)
  val rd     = io.inst(11, 7)
  val funct3 = io.inst(14, 12)
  val rs1    = io.inst(19, 15)
  val rs2    = io.inst(24, 20)
  val funct7 = io.inst(31, 25)

  val immI = io.inst(31, 20).asSInt.pad(32).asUInt
  val immS = Cat(io.inst(31, 25), io.inst(11, 7)).asSInt.pad(32).asUInt
  val immB = Cat(io.inst(31), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W)).asSInt.pad(32).asUInt
  val immU = Cat(io.inst(31, 12), 0.U(12.W)).asSInt.pad(32).asUInt
  val immJ = Cat(io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W)).asSInt.pad(32).asUInt

  val defaultCtrl = Ctrl().toBundle
  val baseR       = Ctrl(op1Sel = OP1_RS1, op2Sel = OP2_RS2, rdSel = RD_ALU, regWen = Y)
  val baseI       = Ctrl(immSel = IMM_I, op1Sel = OP1_RS1, op2Sel = OP2_IMM, rdSel = RD_ALU, regWen = Y)
  val baseLoad    = Ctrl(immSel = IMM_I, op1Sel = OP1_RS1, op2Sel = OP2_IMM, rdSel = RD_MEM, regWen = Y, memR = Y, aluOp = ALU_ADD)
  val baseStore   = Ctrl(immSel = IMM_S, op1Sel = OP1_RS1, op2Sel = OP2_IMM, memWen = Y, aluOp = ALU_ADD)
  val baseBranch  = Ctrl(immSel = IMM_B, op1Sel = OP1_RS1, op2Sel = OP2_RS2, pcSel = PC_BRANCH)

  val decodeTable = Seq(
    // R-type
    ADD  -> baseR.copy(aluOp = ALU_ADD).toBundle,
    SUB  -> baseR.copy(aluOp = ALU_SUB).toBundle,
    XOR  -> baseR.copy(aluOp = ALU_XOR).toBundle,
    OR   -> baseR.copy(aluOp = ALU_OR).toBundle,
    AND  -> baseR.copy(aluOp = ALU_AND).toBundle,
    SLL  -> baseR.copy(aluOp = ALU_LL).toBundle,
    SRL  -> baseR.copy(aluOp = ALU_RL).toBundle,
    SRA  -> baseR.copy(aluOp = ALU_RA).toBundle,
    SLT  -> baseR.copy(aluOp = ALU_LT).toBundle,
    SLTU -> baseR.copy(aluOp = ALU_LTU).toBundle,

    // I-type
    ADDI  -> baseI.copy(aluOp = ALU_ADD).toBundle,
    ANDI  -> baseI.copy(aluOp = ALU_AND).toBundle,
    ORI   -> baseI.copy(aluOp = ALU_OR).toBundle,
    XORI  -> baseI.copy(aluOp = ALU_XOR).toBundle,
    SLLI  -> baseI.copy(aluOp = ALU_LL).toBundle,
    SRLI  -> baseI.copy(aluOp = ALU_RL).toBundle,
    SRAI  -> baseI.copy(aluOp = ALU_RA).toBundle,
    SLTI  -> baseI.copy(aluOp = ALU_LT).toBundle,
    SLTIU -> baseI.copy(aluOp = ALU_LTU).toBundle,

    // Loads
    LB  -> baseLoad.copy(memLen = LEN_BYTE, memSext = Y).toBundle,
    LH  -> baseLoad.copy(memLen = LEN_HALF, memSext = Y).toBundle,
    LW  -> baseLoad.copy(memLen = LEN_WORD, memSext = N).toBundle,
    LBU -> baseLoad.copy(memLen = LEN_BYTE, memSext = N).toBundle,
    LHU -> baseLoad.copy(memLen = LEN_HALF, memSext = N).toBundle,

    // Stores
    SB -> baseStore.copy(memLen = LEN_BYTE).toBundle,
    SH -> baseStore.copy(memLen = LEN_HALF).toBundle,
    SW -> baseStore.copy(memLen = LEN_WORD).toBundle,

    // Branches
    BEQ  -> baseBranch.copy(aluOp = ALU_EQ).toBundle,
    BNE  -> baseBranch.copy(aluOp = ALU_NEQ).toBundle,
    BLT  -> baseBranch.copy(aluOp = ALU_LT).toBundle,
    BGE  -> baseBranch.copy(aluOp = ALU_GE).toBundle,
    BLTU -> baseBranch.copy(aluOp = ALU_LTU).toBundle,
    BGEU -> baseBranch.copy(aluOp = ALU_GEU).toBundle,

    // U-type
    LUI   -> Ctrl(immSel = IMM_U, rdSel = RD_IMM, regWen = Y).toBundle,
    AUIPC -> Ctrl(immSel = IMM_U, op1Sel = OP1_PC, op2Sel = OP2_IMM, rdSel = RD_ALU, regWen = Y, aluOp = ALU_ADD).toBundle,

    // Jumps
    JAL  -> Ctrl(immSel = IMM_J, regWen = Y, rdSel = RD_PC4, pcSel = PC_ALU, op1Sel = OP1_PC, op2Sel = OP2_IMM, aluOp = ALU_ADD).toBundle,
    JALR -> Ctrl(immSel = IMM_I, op1Sel = OP1_RS1, op2Sel = OP2_IMM, rdSel = RD_PC4, regWen = Y, aluOp = ALU_ADD, pcSel = PC_ALU1).toBundle,

    // SYSTEM
    EBREAK -> Ctrl(ebreak = Y).toBundle
  )
  io.ctrl   := MuxLookup(io.inst, defaultCtrl)(decodeTable)
  io.imm    := MuxLookup(io.ctrl.immSel, 0.U)(
    Seq(
      IMM_I -> immI,
      IMM_S -> immS,
      IMM_B -> immB,
      IMM_U -> immU,
      IMM_J -> immJ
    )
  )
  

  io.rs1 := rs1
  io.rs2 := rs2
  io.rd  := rd
}
