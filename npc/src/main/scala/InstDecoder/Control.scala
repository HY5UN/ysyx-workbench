package top

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

object ControlConstants {
  val X = BitPat.dontCare(1)
  // bool
  val Y = BitPat("b1")
  val N = BitPat("b0")

  // immSel
  val IMM_X = BitPat.dontCare(3)
  val IMM_I = BitPat("b000")
  val IMM_S = BitPat("b001")
  val IMM_B = BitPat("b010")
  val IMM_U = BitPat("b011")
  val IMM_J = BitPat("b100")

  // aluOp
  val ALU_X   = BitPat.dontCare(4)
  val ALU_ADD = BitPat("b0000")
  val ALU_SUB = BitPat("b0001")
  val ALU_XOR = BitPat("b0010")
  val ALU_OR  = BitPat("b0011")
  val ALU_AND = BitPat("b0100")
  val ALU_LL  = BitPat("b0101")
  val ALU_RL  = BitPat("b0110")
  val ALU_RA  = BitPat("b0111")
  val ALU_LT  = BitPat("b1000")
  val ALU_LTU = BitPat("b1001")
  val ALU_EQ  = BitPat("b1010")
  val ALU_NEQ = BitPat("b1011")
  val ALU_GE  = BitPat("b1100")
  val ALU_GEU = BitPat("b1101")

  // opSel
  val OP_X    = BitPat.dontCare(2)
  val OP1_RS1 = BitPat("b0")
  val OP1_PC  = BitPat("b1")
  val OP2_RS2 = BitPat("b0")
  val OP2_IMM = BitPat("b1")

  // rdSel
  val RD_X   = BitPat.dontCare(2)
  val RD_ALU = BitPat("b00")
  val RD_MEM = BitPat("b01")
  val RD_PC4 = BitPat("b10")
  val RD_IMM = BitPat("b11")

  // memLen
  val MEM_X    = BitPat.dontCare(2)
  val LEN_BYTE = BitPat("b00")
  val LEN_HALF = BitPat("b01")
  val LEN_WORD = BitPat("b10")

  // pcSel
  val PC_X      = BitPat.dontCare(2)
  val PC_4      = BitPat("b00")
  val PC_ALU    = BitPat("b01") // pc + imm
  val PC_ALU1   = BitPat("b10") // ALU结果低位清0，用于jalr
  val PC_BRANCH = BitPat("b11") // 分支指令，根据比较结果选择pc+4或pc+imm

}

import ControlConstants._

class CtrlBundle extends Bundle {
  val immSel  = UInt(3.W)
  val aluOp   = UInt(4.W)
  val op1Sel  = UInt(2.W)
  val op2Sel  = UInt(2.W)
  val rdSel   = UInt(2.W)
  val regWen  = Bool()
  val memR    = Bool()
  val memWen  = Bool()
  val memLen  = UInt(2.W)
  val memSext = Bool()
  val pcSel   = UInt(2.W)
  val ebreak  = Bool()
}

case class Ctrl(
  immSel:  BitPat = IMM_X,
  aluOp:   BitPat = ALU_X,
  op1Sel:  BitPat = OP_X,
  op2Sel:  BitPat = OP_X,
  rdSel:   BitPat = RD_X,
  regWen:  BitPat = N,
  memR:    BitPat = N,
  memWen:  BitPat = N,
  memLen:  BitPat = MEM_X,
  memSext: BitPat = X,
  pcSel:   BitPat = PC_X,
  ebreak: BitPat = N) {
  def toBitPat: BitPat =
    immSel ## aluOp ## op1Sel ## op2Sel ## rdSel ## regWen ## memR ## memWen ## memLen ## memSext ## pcSel ## ebreak
}
