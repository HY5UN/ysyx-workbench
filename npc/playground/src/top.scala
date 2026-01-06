package ex
import chisel3._
import chisel3.util._   
class top extends Module {
  val io = IO(new Bundle {
    val x=Input(UInt(2.W))
    val en = Input(UInt(1.W))
    val out = Output(UInt(4.W))
  })

  when(io.en){
    switch(io.x){
      is(0.U){io.out:="b1000".U}
      is(1.U){io.out:="b0100".U}
      is(2.U){io.out:="b0010".U}
      is(3.U){io.out:="b0001".U}
    }
  }

}