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

  val keydownReg = RegInit(false.B)

  val gotByte = RegInit(false.B)
  val dataReg = RegInit(0.U(8.W))
  val readyReg = RegInit(false.B)
  readyReg := rx.io.ready
  

  when(readyReg) {
    readyReg := false.B
    dataReg := rx.io.data
    rx.io.nextdata_n := false.B
    gotByte := true.B
  }.otherwise {
    rx.io.nextdata_n := true.B
  }

  when(gotByte) {
    when(rx.io.data === "hF0".U) {
      keydownReg := false.B
    }.otherwise {
      when(keydownReg === false.B) {
        keydownReg := true.B

      }
    }
    gotByte := false.B
  }
  io.keydown := keydownReg
  // io.keydown := rx.io.ready
  when(keydownReg) {
    io.hex(0) := SevenSeg.encodeHex0toF(dataReg(3, 0), true.B)
    io.hex(1) := SevenSeg.encodeHex0toF(dataReg(7, 4), true.B)
  }.otherwise {
    io.hex(0) := SevenSeg.encodeHex0toF(0.U, false.B)
    io.hex(1) := SevenSeg.encodeHex0toF(0.U, false.B)
  }
  for (i <- 2 until 6) {
    io.hex(i) := SevenSeg.encodeHex0toF(0.U, false.B)
  }

}
