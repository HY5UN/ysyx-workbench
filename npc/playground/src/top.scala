package lab
import chisel3._
import chisel3.util._   

class top extends Module {
  val io = IO(new Bundle {
    val step=Input(Bool())
    val hex0 = Output(UInt(7.W))
    val hex1 = Output(UInt(7.W))
    val led = Output(UInt(8.W))
  })
  val digits = RegInit(1.U(8.W))

  when (io.step) {
    digits := digits + 1.U
  }

  val fb =digits(4)^digits(3)^digits(2)^digits(0)
  val next =Cat(fb,digits(7,1))

  digits := Mux(io.step, next, digits)

  io.hex0 := SevenSeg.encodeHex0toF(digits(3,0), true.B)
  io.hex1 := SevenSeg.encodeHex0toF(digits(7,4), true.B)
  io.led := digits
}

// top=top

// io_a (SW3,SW2,SW1,SW0)
// io_b (SW7,SW6,SW5,SW4)
// io_op (SW10,SW9,SW8)
// io_out (LD3,LD2,LD1,LD0)
// io_zero LD5
// io_carry LD6
// io_overflow LD7
