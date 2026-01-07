package lab
import chisel3._
import chisel3.util._   


class top extends Module {
  val io = IO(new Bundle {
    val in =Input(UInt(8.W))
    val led = Output(UInt(3.W))
    val valid = Output(Bool())
    val hex0=Output(UInt(7.W))
  })

  val enc = Module(new PriorityEnc8to3)
  io.valid := enc.io.valid
  enc.io.in := io.in
  io.led := enc.io.out

  io.hex0 := SevenSeg.encodeDigit0to7(enc.io.out, io.valid)
}

// top=top

// io_in (SW7,SW6,SW5,SW4,SW3,SW2,SW1,SW0)
// io_led (LD2,LD1,LD0)
// io_valid LD4
// io_hex0 (SEG0G,SEG0F,SEG0E,SEG0D,SEG0C,SEG0B,SEG0A)