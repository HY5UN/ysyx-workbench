package lab
import chisel3._
import chisel3.util._

class PriorityEnc8to3 extends Module {
  val io = IO(new Bundle {
    val in =Input(UInt(8.W))
    val out = Output(UInt(3.W))
    val valid = Output(Bool())
  })

  val rev= Reverse(io.in)
	val ohRev = PriorityEncoderOH(rev)
	val oh = Reverse(ohRev)
	io.out := OHToUInt(oh)

  io.valid := io.in.orR
}