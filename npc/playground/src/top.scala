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
  // .otherwise {
  //   nextdata_nReg := true.B
  // }

  //temp
  val hf =RegInit(false.B)
  val f0found =RegInit(false.B)
  when(gotByte) {
    when(dataReg==="hF0".U){
      f0found := true.B
    }
    when(f0found&&dataReg===firstByte){ 
      keydownReg := false.B
      hf:= true.B
      f0found := false.B
    }.otherwise {
      when(keydownReg === false.B) {
        keydownReg := true.B

      }
    }
    gotByte := false.B
  }

  //io.keydown := keydownReg
  io.keydown := hf
  when(keydownReg&&rx.io.ready){ {
    io.hex(0) := SevenSeg.encodeHex0toF(firstByte(3, 0), true.B)
    io.hex(1) := SevenSeg.encodeHex0toF(firstByte(7, 4), true.B)
  }.otherwise {
    io.hex(0) := SevenSeg.encodeHex0toF(0.U, false.B)
    io.hex(1) := SevenSeg.encodeHex0toF(0.U, false.B)
  }
  for (i <- 2 until 6) {
    io.hex(i) := SevenSeg.encodeHex0toF(0.U, false.B)
  }
  
}
