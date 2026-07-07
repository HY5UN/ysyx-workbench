package top
import chisel3._
import chisel3.util._

class IFU2ICA extends Bundle {
  val pc  = UInt(32.W)
  val pc4 = UInt(32.W)
}

class IFU extends Module {
  val io  = IO(new Bundle {
    val out        = Decoupled(new IFU2ICA)
    val redirectEn = Input(Bool)
    val redirectPc = Input(UInt(32.W))

  })
  val pc  = RegInit("h30000000".U(32.W))
  val pc4 = WireInit((pc + 4.U)(31, 0))
  io.out.bits.pc := pc
  io.out.valid   := false.B

  when(io.redirectEn) {
    pc           := io.redirectPc
  }.otherwise {
    io.out.valid := true.B
    when(io.out.ready) {
      pc := pc4
    }
  }
}
