package lab
import chisel3._
import chisel3.util._   


class top extends Module {
  val io = IO(new Bundle {
    val in =Input(UInt(8.W))
    val led = Output(UInt(3.W))
    val valid = Output(Bool())
    val hex0=Output(UInt(7.W))
  })

  val enc = Module(new PriorityEnc8to3)
  valid := enc.io.valid
  enc.io.in := io.in
  io.led := enc.io.out

  io.hex0 := SevenSeg.encodeDigit0to7(enc.io.out)
}