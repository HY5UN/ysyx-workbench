package ex
import chisel3._
import chisel3.util._   
class top extends Module {
  val io = IO(new Bundle {
    val x=Input(UInt(2.W))
    val en = Input(Bool())
    val out = Output(UInt(4.W))
  })

  io.out := 0.U

  io.out:=Mux(
    io.en,
    MuxLookup(io.x, 0.U, Array(
      0.U -> "0001".U,
      1.U -> "0010".U,
      2.U -> "0100".U,
      3.U -> "1000".U
    )),
    0.U
  )

}