

import chisel3._
import chisel3.util._   
class Mux41 extends Module {
  val io = IO(new Bundle {
    val in0 = Input(UInt(1.W))
    val in1 = Input(UInt(1.W))
    val in2 = Input(UInt(1.W))
    val in3 = Input(UInt(1.W))
    val sel = Input(UInt(2.W))
    val out = Output(UInt(1.W))
  })

    io.out := MuxLookup(io.sel, 0.U, Array(
        0.U -> io.in0,
        1.U -> io.in1,
        2.U -> io.in2,
        3.U -> io.in3
    ))

}