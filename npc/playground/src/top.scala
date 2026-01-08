package lab
import chisel3._
import chisel3.util._

class top extends Module {
  val io = IO(new Bundle {
    val ps2clk  = Input(Bool())
    val ps2data = Input(Bool())

    val hex     = Output(Vec(6, UInt(7.W)))
    // temp
    val led0 = Output(Bool())
  })

  val rx = Module(new PS2KeyboardRx)
  rx.io.ps2clk  := io.ps2clk
  rx.io.ps2data := io.ps2data

  val keydownReg = RegInit(false.B)

  val gotByte       = RegInit(false.B)
  val dataReg       = RegInit(0.U(8.W))
  val nextdata_nReg = RegInit(true.B)
  nextdata_nReg    := true.B
  rx.io.nextdata_n := nextdata_nReg

  val firstByte = RegInit(0.U(8.W))
  when(rx.io.ready) {
    gotByte := true.B

    nextdata_nReg := false.B
    dataReg       := rx.io.data
    when(!keydownReg) {

      firstByte := rx.io.data
    }
  }

  val keyCounter = RegInit(0.U(8.W))
  
  val f0found = RegInit(false.B)
  when(gotByte) {
    when(dataReg === "hF0".U) {
      f0found := true.B
    }.otherwise {
      when(f0found && dataReg === firstByte) {
        keydownReg := false.B
        hf         := true.B
        f0found    := false.B
      }.otherwise {
        when(keydownReg === false.B) {
          keydownReg := true.B
          keyCounter := keyCounter + 1.U
        }
      }
    }
    gotByte := false.B
  }

  when(keydownReg) {
    io.hex(0) := SevenSeg.encodeHex0toF(firstByte(3, 0), true.B)
    io.hex(1) := SevenSeg.encodeHex0toF(firstByte(7, 4), true.B)
  }.otherwise {
    io.hex(0) := SevenSeg.encodeHex0toF(0.U, false.B)
    io.hex(1) := SevenSeg.encodeHex0toF(0.U, false.B)
  }

  
  //temp
  io.hex(2) := SevenSeg.encodeHex0toF(dataReg(3, 0), true.B)
  io.hex(3) := SevenSeg.encodeHex0toF(dataReg(7, 4), true.B)

  io.hex(4) := SevenSeg.encodeHex0toF(keyCounter(3, 0), true.B)
  io.hex(5) := SevenSeg.encodeHex0toF(keyCounter(7, 4), true.B)

}
