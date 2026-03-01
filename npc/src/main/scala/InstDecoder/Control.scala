package top

import chisel3._
import chisel3.util._

object ControlConstants {
  val X = 0.U
  // bool
  val Y = 1.U(1.W)
  val N = 0.U(1.W)

  // immSel
  val IMM_I = "b000".U(3.W)
  val IMM_S = "b001".U(3.W)
  val IMM_B = "b010".U(3.W)
  val IMM_U = "b011".U(3.W)
  val IMM_J = "b100".U(3.W)

  // aluOp
  val ALU_ADD = "b0000".U(4.W)
  val ALU_SUB = "b0001".U(4.W)
  val ALU_XOR = "b0010".U(4.W)
  val ALU_OR  = "b0011".U(4.W)
  val ALU_AND = "b0100".U(4.W)
  val ALU_LL  = "b0101".U(4.W)
  val ALU_RL  = "b0110".U(4.W)
  val ALU_RA  = "b0111".U(4.W)
  val ALU_LT  = "b1000".U(4.W)
  val ALU_LTU = "b1001".U(4.W)
  val ALU_EQ  = "b1010".U(4.W)
  val ALU_NEQ = "b1011".U(4.W)
  val ALU_GE  = "b1100".U(4.W)
  val ALU_GEU = "b1101".U(4.W)

  // opSel
  val OP1_RS1 = "b0".U(1.W)
  val OP1_PC  = "b1".U(1.W)
  val OP2_RS2 = "b0".U(1.W)
  val OP2_IMM = "b1".U(1.W)

  // rdSel
  val RD_ALU = "b00".U(2.W)
  val RD_MEM = "b01".U(2.W)
  val RD_PC4 = "b10".U(2.W)
  val RD_IMM = "b11".U(2.W)

  // memLen
  val LEN_BYTE = "b00".U(2.W)
  val LEN_HALF = "b01".U(2.W)
  val LEN_WORD = "b10".U(2.W)

  // pcSel
  val PC_4      = "b00".U(2.W)
  val PC_ALU    = "b01".U(2.W) // pc + imm
  val PC_ALU1   = "b10".U(2.W) // ALU结果低位清0，用于jalr
  val PC_BRANCH = "b11".U(2.W) // 分支指令，根据比较结果选择pc+4或pc+imm

}

import ControlConstants._

case class Ctrl(
  immSel:  UInt = X,
  aluOp:   UInt = X,
  op1Sel:  UInt = X,
  op2Sel:  UInt = X,
  rdSel:   UInt = X,
  regWen:  UInt = N,
  memR:    UInt = N,
  memWen:  UInt = N,
  memLen:  UInt = X,
  memSext: UInt = N,
  pcSel:   UInt = X,
  ebreak: UInt = N) {
  def toList: List[UInt] =
    List(immSel, aluOp, op1Sel, op2Sel, rdSel, regWen, memR, memWen, memLen, memSext, pcSel, ebreak)
}
