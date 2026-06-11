package top
import chisel3._
import chisel3.util._

class MemExt extends ExtModule {
  val io = IO(new Bundle {
    // val clock = Input(Clock())
    // val reset = Input(Bool())
    val axi   = Flipped(new AXI4IO)
  })
}