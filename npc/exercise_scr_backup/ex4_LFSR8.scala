package lab
import chisel3._
import chisel3.util._   

class top extends Module {
  val io = IO(new Bundle {
    val hex0 = Output(UInt(7.W))
    val hex1 = Output(UInt(7.W))
    val led = Output(UInt(8.W))
  })
  val digits = RegInit(1.U(8.W))

  

  val fb =digits(4)^digits(3)^digits(2)^digits(0)
  val nextN =Cat(fb,digits(7,1))
  val nextZ =1.U(8.W)
  val next= Mux(digits===0.U,nextZ,nextN)

  digits := next

  io.hex0 := SevenSeg.encodeHex0toF(digits(3,0), true.B)
  io.hex1 := SevenSeg.encodeHex0toF(digits(7,4), true.B)
  io.led := digits
}

// top=top

// clock BTNC
// reset BTNR
// io_hex0 (SEG0G,SEG0F,SEG0E,SEG0D,SEG0C,SEG0B,SEG0A)
// io_hex1 (SEG1G,SEG1F,SEG1E,SEG1D,SEG1C,SEG1B,SEG1A)
// io_led (LD7,LD6,LD5,LD4,LD3,LD2,LD1,LD0)
// 还要注释掉main.cpp里面的single_cycle