

import chisel3._
import chisel3.util._   
class Mux41 extends Module {
  val io = IO(new Bundle {
    val in=Input(UInt(4.W))
    val sel = Input(UInt(2.W))
    val out = Output(UInt(1.W))
  })

  io.out := io.in(io.sel);
}