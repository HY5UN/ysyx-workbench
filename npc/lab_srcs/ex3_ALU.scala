package lab
import chisel3._
import chisel3.util._   

//ALU
class top extends Module {
  val io = IO(new Bundle {
    val a = Input(UInt(4.W))
    val b = Input(UInt(4.W))
    val op = Input(UInt(3.W))
    val out = Output(UInt(4.W))

    val zero = Output(Bool())
    val carry = Output(Bool())
    val overflow = Output(Bool())
  })

  val addExt = io.a +& io.b;
  val addRes= addExt(3,0)
  val addCarry= addExt(4)

  val subExt = io.a +& (~io.b + 1.U)
  val subRes= subExt(3,0)
  val subC= subExt(4)

  val ovAdd= (io.a(3) === io.b(3)) && (addRes(3) =/= io.a(3))
  val ovSub= (io.a(3) =/= io.b(3)) && (subRes(3) =/= io.a(3))

  val notA= ~io.a
  val andR= io.a & io.b
  val orR= io.a | io.b
  val xorR= io.a ^ io.b

  val ltSigned= subRes(3) ^ ovSub
  val ltOut=Cat(0.U(3.W), ltSigned)

  val eq =io.a===io.b
  val eqOut= Cat(0.U(3.W), eq)

  val selOh=UIntToOH(io.op,8)
  io.out := Mux1H(Seq(
    selOh(0) -> addRes,
    selOh(1) -> subRes,
    selOh(2) -> notA,
    selOh(3) -> andR,
    selOh(4) -> orR,
    selOh(5) -> xorR,
    selOh(6) -> ltOut,
    selOh(7) -> eqOut
  ))
  
  io.carry := Mux1H(Seq(
    selOh(0) -> addCarry,
    selOh(1) -> (~subC).asBool,
    selOh(2) -> false.B,
    selOh(3) -> false.B,
    selOh(4) -> false.B,
    selOh(5) -> false.B,
    selOh(6) -> false.B,
    selOh(7) -> false.B
  ))

  io.overflow := Mux1H(Seq(
    selOh(0) -> ovAdd,
    selOh(1) -> ovSub,
    selOh(2) -> false.B,
    selOh(3) -> false.B,
    selOh(4) -> false.B,
    selOh(5) -> false.B,
    selOh(6) -> false.B,
    selOh(7) -> false.B
  ))

  io.zero := (io.out === 0.U)
}

// top=top

// io_a (SW3,SW2,SW1,SW0)
// io_b (SW7,SW6,SW5,SW4)
// io_op (SW10,SW9,SW8)
// io_out (LD3,LD2,LD1,LD0)
// io_zero LD5
// io_carry LD6
// io_overflow LD7
