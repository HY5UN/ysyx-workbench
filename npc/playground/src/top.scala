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
  
  
  val keyDown  = RegInit(false.B)
val sawF0    = RegInit(false.B)
val lastCode = RegInit(0.U(8.W))

when(rx.io.ready) {
  when(sawF0) {
    // 这是 break 的第二帧：释放的键码
    sawF0 := false.B
    keyDown := false.B
  }.elsewhen(rx.io.data === "hF0".U) {
    sawF0 := true.B
  }.otherwise {
    // make code
    when(!keyDown) {
      keyDown := true.B
      lastCode := rx.io.data   // 只在首次按下更新显示
    }
    // keyDown=true 时来的重复码直接忽略
  }
}

  when(keyDown) {
    io.hex(0) := SevenSeg.encodeHex0toF(lastCode(3,0), true.B)
    io.hex(1) := SevenSeg.encodeHex0toF(lastCode(7,4), true.B)
  }.otherwise {
    io.hex(0) := SevenSeg.encodeHex0toF(0.U, false.B)
    io.hex(1) := SevenSeg.encodeHex0toF(0.U, false.B)
  }
  //temp
  io.hex(2) := SevenSeg.encodeHex0toF(0.U, false.B)
  io.hex(3) := SevenSeg.encodeHex0toF(0.U, false.B)
  io.hex(4) := SevenSeg.encodeHex0toF(0.U, false.B)
  io.hex(5) := SevenSeg.encodeHex0toF(0.U, false.B)
}
