
import chisel3._

class Mux21 extends Module {
  val io = IO(new Bundle {
    val in0 = Input(UInt(1.W))
    val in1 = Input(UInt(1.W))
    val sel = Input(Bool())
    val out = Output(UInt(1.W))
  })

  io.out := Mux(io.sel, io.in1, io.in0)
}