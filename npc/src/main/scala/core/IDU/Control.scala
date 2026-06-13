package top

import chisel3._
import chisel3.util._
import chisel3.util.experimental.decode._
object ControlConstants {
object ImmSel extends ChiselEnum { val I, S, B, U, J = Value }

object AluOp extends ChiselEnum {
  val ADD, SUB, XOR, OR, AND, LL, RL, RA, LT, LTU, EQ, NEQ, GE, GEU = Value
}

object Op1Sel extends ChiselEnum { val RS1, PC = Value }

object Op2Sel extends ChiselEnum { val RS2, IMM, CSR = Value }

object RdSel extends ChiselEnum { val ALU, MEM, PC4, IMM, CSR = Value }

object MemLen extends ChiselEnum { val BYTE, HALF, WORD = Value }

object PcSel extends ChiselEnum { val NEXT, ALU, ALU1, BRANCH, CSR = Value }

object CsrSel extends ChiselEnum { val RS1, ALU, PC = Value }
}


// ── CtrlBundle：字段类型直接用枚举，宽度自动推导 ─────────────────────────

class CtrlBundle extends Bundle {
  val immSel  = ImmSel()
  val aluOp   = AluOp()
  val op1Sel  = Op1Sel()
  val op2Sel  = Op2Sel()
  val rdSel   = RdSel()
  val regWen  = Bool()
  val memR    = Bool()
  val memWen  = Bool()
  val memLen  = MemLen()
  val memSext = Bool()
  val pcSel   = PcSel()
  val ebreak  = Bool()
  val ecall   = Bool()
  val csrWen  = Bool()
  val csrSel  = CsrSel()
  val mret    = Bool()
}

// ── Ctrl：新增字段只改这里和 CtrlBundle，toList 自动跟随 ─────────────────
// 注意：Ctrl 字段声明顺序必须与 CtrlBundle 保持一致

case class Ctrl(
  immSel:  UInt = ImmSel.I,
  aluOp:   UInt = AluOp.ADD,
  op1Sel:  UInt = Op1Sel.RS1,
  op2Sel:  UInt = Op2Sel.RS2,
  rdSel:   UInt = RdSel.ALU,
  regWen:  Bool = false.B,
  memR:    Bool = false.B,
  memWen:  Bool = false.B,
  memLen:  UInt = MemLen.BYTE,
  memSext: Bool = false.B,
  pcSel:   UInt = PcSel.NEXT,
  ebreak:  Bool = false.B,
  ecall:   Bool = false.B,
  csrWen:  Bool = false.B,
  csrSel:  UInt = CsrSel.RS1,
  mret:    Bool = false.B
) {
  // productIterator 按字段声明顺序迭代，toList 永远不需要手动维护
  def toList: List[UInt] =
    productIterator.map(_.asInstanceOf[UInt]).toList
}