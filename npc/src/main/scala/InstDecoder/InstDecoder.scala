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

    val aluOp        = Output(UInt(4.W))
    val op1Sel       = Output(UInt(1.W))
    val op2Sel       = Output(UInt(1.W))
    val rdSel        = Output(UInt(2.W))
    val regWen       = Output(Bool())
    val memReadValid = Output(Bool())
    val memWen       = Output(Bool())
    val memLen       = Output(UInt(2.W))
    val memSext      = Output(Bool())
    val pcSel        = Output(UInt(2.W))
    val ebreak       = Output(Bool())
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

  val defaultCtrl = Ctrl().toList
  val baseR       = Ctrl(op1Sel = OP1_RS1, op2Sel = OP2_RS2, rdSel = RD_ALU, regWen = Y)
  val baseI       = Ctrl(immSel = IMM_I, op1Sel = OP1_RS1, op2Sel = OP2_IMM, rdSel = RD_ALU, regWen = Y)
  val baseLoad    = Ctrl(immSel = IMM_I, op1Sel = OP1_RS1, op2Sel = OP2_IMM, rdSel = RD_MEM, regWen = Y, memR = Y, aluOp = ALU_ADD)
  val baseStore   = Ctrl(immSel = IMM_S, op1Sel = OP1_RS1, op2Sel = OP2_IMM, memWen = Y, aluOp = ALU_ADD)
  val baseBranch  = Ctrl(immSel = IMM_B, op1Sel = OP1_RS1, op2Sel = OP2_RS2, pcSel = PC_BRANCH)

  val decodeTable = Array(
    // R-type
    ADD  -> baseR.copy(aluOp = ALU_ADD).toList,
    SUB  -> baseR.copy(aluOp = ALU_SUB).toList,
    XOR  -> baseR.copy(aluOp = ALU_XOR).toList,
    OR   -> baseR.copy(aluOp = ALU_OR).toList,
    AND  -> baseR.copy(aluOp = ALU_AND).toList,
    SLL  -> baseR.copy(aluOp = ALU_LL).toList,
    SRL  -> baseR.copy(aluOp = ALU_RL).toList,
    SRA  -> baseR.copy(aluOp = ALU_RA).toList,
    SLT  -> baseR.copy(aluOp = ALU_LT).toList,
    SLTU -> baseR.copy(aluOp = ALU_LTU).toList,

    // I-type
    ADDI  -> baseI.copy(aluOp = ALU_ADD).toList,
    ANDI  -> baseI.copy(aluOp = ALU_AND).toList,
    ORI   -> baseI.copy(aluOp = ALU_OR).toList,
    XORI  -> baseI.copy(aluOp = ALU_XOR).toList,
    SLLI  -> baseI.copy(aluOp = ALU_LL).toList,
    SRLI  -> baseI.copy(aluOp = ALU_RL).toList,
    SRAI  -> baseI.copy(aluOp = ALU_RA).toList,
    SLTI  -> baseI.copy(aluOp = ALU_LT).toList,
    SLTIU -> baseI.copy(aluOp = ALU_LTU).toList,

    // Loads
    LB  -> baseLoad.copy(memLen = LEN_BYTE, memSext = Y).toList,
    LH  -> baseLoad.copy(memLen = LEN_HALF, memSext = Y).toList,
    LW  -> baseLoad.copy(memLen = LEN_WORD, memSext = N).toList,
    LBU -> baseLoad.copy(memLen = LEN_BYTE, memSext = N).toList,
    LHU -> baseLoad.copy(memLen = LEN_HALF, memSext = N).toList,

    // Stores
    SB -> baseStore.copy(memLen = LEN_BYTE).toList,
    SH -> baseStore.copy(memLen = LEN_HALF).toList,
    SW -> baseStore.copy(memLen = LEN_WORD).toList,

    // Branches
    BEQ  -> baseBranch.copy(aluOp = ALU_EQ).toList,
    BNE  -> baseBranch.copy(aluOp = ALU_NEQ).toList,
    BLT  -> baseBranch.copy(aluOp = ALU_LT).toList,
    BGE  -> baseBranch.copy(aluOp = ALU_GE).toList,
    BLTU -> baseBranch.copy(aluOp = ALU_LTU).toList,
    BGEU -> baseBranch.copy(aluOp = ALU_GEU).toList,

    // U-type
    LUI   -> Ctrl(immSel = IMM_U, rdSel = RD_IMM, regWen = Y).toList,
    AUIPC -> Ctrl(immSel = IMM_U, op1Sel = OP1_PC, op2Sel = OP2_IMM, rdSel = RD_ALU, regWen = Y, aluOp = ALU_ADD).toList,

    // Jumps
    JAL  -> Ctrl(immSel = IMM_J, regWen = Y, rdSel = RD_PC4, pcSel = PC_ALU, op1Sel = OP1_PC, op2Sel = OP2_IMM, aluOp = ALU_ADD).toList,
    JALR -> Ctrl(immSel = IMM_I, op1Sel = OP1_RS1, op2Sel = OP2_IMM, rdSel = RD_PC4, regWen = Y, aluOp = ALU_ADD, pcSel = PC_ALU1).toList,

    // SYSTEM
    EBREAK -> Ctrl(ebreak = Y).toList
  )
  val ctrlSignals =ListLookup(io.inst, defaultCtrl, decodeTable)
  val immSel::aluOp::op1Sel::op2Sel::rdSel::regWen::memR::memWen::memLen::memSext::pcSel::ebreak::Nil = ctrlSignals

  io.imm    := MuxLookup(immSel, 0.U)(
    Seq(
      IMM_I -> immI,
      IMM_S -> immS,
      IMM_B -> immB,
      IMM_U -> immU,
      IMM_J -> immJ
    )
  )
  io.aluOp   := aluOp
  io.op1Sel  := op1Sel  
  io.op2Sel  := op2Sel
  io.rdSel   := rdSel
  io.regWen  := regWen
  io.memReadValid := memR
  io.memWen  := memWen
  io.memLen  := memLen
  io.memSext := memSext
  io.pcSel   := pcSel
  io.ebreak  := ebreak

  io.rs1 := rs1
  io.rs2 := rs2
  io.rd  := rd
}
