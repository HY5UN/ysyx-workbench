
//Mux4to1_2bit 
import chisel3._
import chisel3.util._   
class top extends Module {
  val io = IO(new Bundle {
    val in=Input(Vec(4, UInt(2.W)))
    val sel = Input(UInt(2.W))
    val out = Output(UInt(2.W))
  })

  io.out := io.in(io.sel);
}