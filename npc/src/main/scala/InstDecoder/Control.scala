package top

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._

object ControlConstants {
  val X = "b0".U
  // bool
  val Y = "b1".U
  val N = "b0".U

  // immSel
  val IMM_I = "b000".U
  val IMM_S = "b001".U
  val IMM_B = "b010".U
  val IMM_U = "b011".U
  val IMM_J = "b100".U

  // aluOp
  val ALU_ADD = "b0000".U
  val ALU_SUB = "b0001".U
  val ALU_XOR = "b0010".U
  val ALU_OR  = "b0011".U
  val ALU_AND = "b0100".U
  val ALU_LL  = "b0101".U
  val ALU_RL  = "b0110".U
  val ALU_RA  = "b0111".U
  val ALU_LT  = "b1000".U
  val ALU_LTU = "b1001".U
  val ALU_EQ  = "b1010".U
  val ALU_NEQ = "b1011".U
  val ALU_GE  = "b1100".U
  val ALU_GEU = "b1101".U

  // opSel
  val OP1_RS1 = "b0".U
  val OP1_PC  = "b1".U

  val OP2_RS2 = "b00".U
  val OP2_IMM = "b01".U
  val OP2_CSR = "b10".U

  // rdSel
  val RD_ALU = "b000".U
  val RD_MEM = "b001".U
  val RD_PC4 = "b010".U
  val RD_IMM = "b011".U
  val RD_CSR = "b100".U

  // memLen
  val LEN_BYTE = "b00".U
  val LEN_HALF = "b01".U
  val LEN_WORD = "b10".U

  // pcSel
  val PC_4      = "b000".U
  val PC_ALU    = "b001".U // pc + imm
  val PC_ALU1   = "b010".U // ALU结果低位清0，用于jalr
  val PC_BRANCH = "b011".U // 分支指令，根据比较结果选择pc+4或pc+imm
  val PC_CSR    = "b100".U // CSR指令，使用CSR中的地址

  // csrSel
  val CSR_RS1 = "b00".U
  val CSR_ALU = "b01".U
}

import ControlConstants._

class CtrlBundle extends Bundle {
  val immSel  = UInt(3.W)
  val aluOp   = UInt(4.W)
  val op1Sel  = UInt(1.W)
  val op2Sel  = UInt(2.W)
  val rdSel   = UInt(3.W)
  val regWen  = Bool()
  val memR    = Bool()
  val memWen  = Bool()
  val memLen  = UInt(2.W)
  val memSext = Bool()
  val pcSel   = UInt(3.W)
  val ebreak  = Bool()
  val ecall   = Bool()
  val csrWen  = Bool()
  val csrSel  = UInt(2.W)
  val mret    = Bool()
}

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
  ebreak:  UInt = N,
  ecall:   UInt = N,
  csrWen:  UInt = N,
  csrSel:  UInt = X,
  mret: UInt = N) {

  def toList: List[UInt] =
    List(
      immSel,
      aluOp,
      op1Sel,
      op2Sel,
      rdSel,
      regWen,
      memR,
      memWen,
      memLen,
      memSext,
      pcSel,
      ebreak,
      ecall,
      csrWen,
      csrSel,
      mret
    )

}
