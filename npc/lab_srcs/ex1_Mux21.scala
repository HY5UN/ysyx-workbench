package ex
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

// 放在top.ndxc

// top=top

// io_in (SW3,SW2,SW1,SW0)
// io_sel (SW15,SW14)
// io_out LD0