package top

import chisel3._
import chisel3.util._

object ImmSel extends ChiselEnum { val I, S, B, U, J = Value }

object AluOp extends ChiselEnum {
  val ADD, SUB, XOR, OR, AND, LL, RL, RA, LT, LTU, EQ, NEQ, GE, GEU = Value
}

object Op1Sel extends ChiselEnum { val RS1, PC = Value }

object Op2Sel extends ChiselEnum { val RS2, IMM, CSR = Value }

object RdSel extends ChiselEnum { val ALU, MEM, PC4, IMM, CSR = Value }

// object MemLen extends ChiselEnum { val BYTE, HALF, WORD = Value }
object MemLen  { val BYTE = "b00".U;val HALF = "b01".U;val WORD = "b10".U }

object PcSel extends ChiselEnum { val NEXT, ALU, ALU1, BRANCH, CSR = Value }

object CsrSel extends ChiselEnum { val RS1, ALU, PC = Value }


class CtrlBundle extends Bundle {
  val immSel  = ImmSel()
  val aluOp   = AluOp()
  val op1Sel  = Op1Sel()
  val op2Sel  = Op2Sel()
  val rdSel   = RdSel()
  val regWen  = Bool()
  val memR    = Bool()
  val memWen  = Bool()
  val memLen  = UInt(2.W)
  val memSext = Bool()
  val pcSel   = PcSel()
  val ebreak  = Bool()
  val ecall   = Bool()
  val csrWen  = Bool()
  val csrSel  = CsrSel()
  val mret    = Bool()
}

case class Ctrl(
  immSel:  ImmSel.Type  = ImmSel.I,
  aluOp:   AluOp.Type   = AluOp.ADD,
  op1Sel:  Op1Sel.Type  = Op1Sel.RS1,
  op2Sel:  Op2Sel.Type  = Op2Sel.RS2,
  rdSel:   RdSel.Type   = RdSel.ALU,
  regWen:  Bool         = false.B,
  memR:    Bool         = false.B,
  memWen:  Bool         = false.B,
  memLen:  UInt         = MemLen.BYTE,
  memSext: Bool         = false.B,
  pcSel:   PcSel.Type   = PcSel.NEXT,
  ebreak:  Bool         = false.B,
  ecall:   Bool         = false.B,
  csrWen:  Bool         = false.B,
  csrSel:  CsrSel.Type  = CsrSel.RS1,
  mret:    Bool         = false.B
) {
  def toList: List[UInt] =
    productIterator.map(_.asInstanceOf[Data].asUInt).toList
}