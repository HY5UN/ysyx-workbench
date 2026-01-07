package lab
import chisel3._
import chisel3.util._

class top extends Module {
  val io = IO(new Bundle {
    val ps2clk  = Input(Bool())
    val ps2data = Input(Bool())
    val hex     = Output(Vec(6, UInt(7.W)))
    val keydown = Output(Bool())
  })

  val rx = Module(new PS2KeyboardRx)
  rx.io.ps2clk  := io.ps2clk
  rx.io.ps2data := io.ps2data

  val keyDown  = RegInit(false.B)
  val sawF0    = RegInit(false.B)
  val lastCode = RegInit(0.U(8.W))

  when(rx.io.ready) {
    when(sawF0) {
      // 这是 break 的第二字节：释放某键
      sawF0   := false.B
      keyDown := false.B
      lastCode := 0.U
    }.elsewhen(rx.io.data === "hF0".U) {
      // break 前缀
      sawF0 := true.B
    }.otherwise {
      // make code
      when(!keyDown) {
        keyDown  := true.B
        lastCode := rx.io.data
      }.otherwise {
        // 按住不放会重复发 make code：这里选择忽略，不更新 lastCode
        // 如果你希望显示一直刷新，也可以 lastCode := rx.io.data
      }
    }
  }

  io.keydown := keyDown

  // 低两位显示当前按下键码；松开则灭
  when(keyDown) {
    io.hex(0) := SevenSeg.encodeHex0toF(lastCode(3, 0), true.B)
    io.hex(1) := SevenSeg.encodeHex0toF(lastCode(7, 4), true.B)
  }.otherwise {
    io.hex(0) := SevenSeg.encodeHex0toF(0.U, false.B)
    io.hex(1) := SevenSeg.encodeHex0toF(0.U, false.B)
  }

  // 你下面的 temp 显示：不要每拍都 code := rx.io.data（会显示到“非ready时的旧值/垃圾语义”）
  // 如果你只是想显示“最近一次收到的字节”，也应该只在 ready 时更新：
  val code = RegInit(0.U(8.W))
  when(rx.io.ready) { code := rx.io.data }
  io.hex(2) := SevenSeg.encodeHex0toF(code(3, 0), true.B)
  io.hex(3) := SevenSeg.encodeHex0toF(code(7, 4), true.B)

  io.hex(4) := SevenSeg.encodeHex0toF(0.U, false.B)
  io.hex(5) := SevenSeg.encodeHex0toF(0.U, false.B)
}
