package lab
import chisel3._
import chisel3.util._

class top extends Module {
  val io = IO(new Bundle {
    val ps2clk  = Input(Bool())
    val ps2data = Input(Bool())

    val hex     = Output(Vec(6, UInt(7.W)))
    // temp
    val keydown = Output(Bool())
  })

  val rx = Module(new PS2KeyboardRx)
  rx.io.ps2clk  := io.ps2clk
  rx.io.ps2data := io.ps2data

  val keyDown  = RegInit(false.B)
  val lastCode = RegInit(0.U(8.W))
  val display  = RegInit(false.B)
  display    := false.B
  when(rx.io.ready) {
    lastCode := rx.io.data
    when(rx.io.data === "hF0".U) {
      keyDown := false.B
      display := true.B
    }.otherwise {
      when(!keyDown) {
        keyDown := true.B
        display := true.B
      }
    }
  }
  io.keydown := keyDown
  val hexReg = RegInit(VecInit(Seq.fill(6)(0.U(7.W))))
  io.hex := hexReg
  when(display) {

    when(keyDown) {
      hexReg(0) := SevenSeg.encodeHex0toF(lastCode(3, 0), true.B)
      hexReg(1) := SevenSeg.encodeHex0toF(lastCode(7, 4), true.B)
    }.otherwise {
      hexReg(0) := SevenSeg.encodeHex0toF(0.U, false.B)
      hexReg(1) := SevenSeg.encodeHex0toF(0.U, false.B)
    }
  }
  // temp
  val code = RegInit(0.U(8.W))
  code       := rx.io.data
  io.hex(2)  := SevenSeg.encodeHex0toF(code(3, 0), true.B)
  io.hex(3)  := SevenSeg.encodeHex0toF(code(7, 4), true.B)
  io.hex(4)  := SevenSeg.encodeHex0toF(0.U, false.B)
  io.hex(5)  := SevenSeg.encodeHex0toF(0.U, false.B)
}
