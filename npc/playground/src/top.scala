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
val sawF0    = RegInit(false.B)

when(rx.io.ready) {
  val b = rx.io.data

  when(sawF0) {
    // 这一帧是 break 的“键码帧”，表示释放
    sawF0 := false.B
    keyDown := false.B
    lastCode := 0.U // 可选：清掉
  }.elsewhen(b === "hF0".U) {
    // break 前缀
    sawF0 := true.B
  }.otherwise {
    // make code
    lastCode := b
    when(!keyDown) {
      keyDown := true.B
      // 这里如果要计数，也只在 !keyDown 时 +1
    }.otherwise {
      // 按住产生重复码：忽略（不计数不更新也行）
      // 如果你想更新 lastCode 也可以，但不建议
    }
  }
}

io.keydown := keyDown

when(keyDown) {
  io.hex(0) := SevenSeg.encodeHex0toF(lastCode(3, 0), true.B)
  io.hex(1) := SevenSeg.encodeHex0toF(lastCode(7, 4), true.B)
}.otherwise {
  io.hex(0) := SevenSeg.encodeHex0toF(0.U, false.B)
  io.hex(1) := SevenSeg.encodeHex0toF(0.U, false.B)
}
  // temp
  val code = RegInit(0.U(8.W))
   code := rx.io.data
  io.hex(2) := SevenSeg.encodeHex0toF(code(3, 0), true.B)
  io.hex(3) := SevenSeg.encodeHex0toF(code(7, 4), true.B)
  io.hex(4) := SevenSeg.encodeHex0toF(0.U, false.B)
  io.hex(5) := SevenSeg.encodeHex0toF(0.U, false.B)
}
