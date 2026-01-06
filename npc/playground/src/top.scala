package ex
import chisel3._
import chisel3.util._   
class top extends Module {
  val io = IO(new Bundle {
    val x=Input(UInt(2.W))
    val en = Input(Bool())
    val out = Output(UInt(4.W))
  })

  io.out:=Mux(
    io.en,
    MuxLookup(io.x, 0.U) (Seq(
      0.U -> "b0001".U,
      1.U -> "b0010".U,
      2.U -> "b0100".U,
      3.U -> "b1000".U
    )),
    0.U
  )

}