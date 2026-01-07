package lab
import chisel3._
import chisel3.util._   

class top extends Module {
  val io = IO(new Bundle {
    val ps2clk= Input(Bool())
    val ps2data= Input(Bool())

    val hex= Output(Vec(6, UInt(7.W)))
  })

  val rx = Module(new PS2KeyboardRx)
  rx.io.ps2clk := io.ps2clk
  rx.io.ps2data := io.ps2data
  
  
  val keyDown = RegInit(false.B)

  val lastCode = RegInit(0.U(8.W))
  when(rx.io.ready) {
    lastCode := rx.io.data
    when(rx.io.data === "hF0".U) {
      keyDown := false.B
    }.otherwise {
      when(!keyDown) {
        keyDown := true.B
      }
    }
  }

  when(keyDown) {
    io.hex(0) := SevenSeg.encodeHex0toF(lastCode(3,0), true.B)
    io.hex(1) := SevenSeg.encodeHex0toF(lastCode(7,4), true.B)
  }.otherwise {
    io.hex(0) := SevenSeg.encodeHex0toF(0.U, false.B)
    io.hex(1) := SevenSeg.encodeHex0toF(0.U, false.B)
  }
}
