package lab
import chisel3._
import chisel3.util._

class PS2KeyboardRx extends Module {
  val io = IO(new Bundle {
    val ps2clk  = Input(Bool())
    val ps2data = Input(Bool())
    val data    = Output(UInt(8.W))
    val ready   = Output(Bool())
  })

  // 同步+下降沿检测
  val ps2ClkSync = RegInit(7.U(3.W))                 // 111
  ps2ClkSync := Cat(ps2ClkSync(1, 0), io.ps2clk.asUInt)
  val sampling = ps2ClkSync(2) && !ps2ClkSync(1)     // falling edge

  val count  = RegInit(0.U(4.W))     // 0..10
  val bits   = RegInit(0.U(11.W))    // start + 8data + parity + stop
  val dataR  = RegInit(0.U(8.W))
  val readyR = RegInit(false.B)

  io.data  := dataR
  io.ready := readyR

  // ready 只打一拍
  readyR := false.B

  when(sampling) {
    val nextBits = bits.bitSet(count, io.ps2data) // ✅ 关键：用 nextBits
    bits := nextBits

    when(count === 10.U) {
      val startOk = nextBits(0) === 0.U
      val data    = nextBits(8, 1)       // LSB-first -> 直接就是扫描码
      val parity  = nextBits(9)
      val stopOk  = nextBits(10) === 1.U

      // odd parity：data XOR parity = 1
      val oddOk   = (data.xorR ^ parity) === 1.B

      when(startOk && stopOk && oddOk) {
        dataR  := data
        readyR := true.B
      }
      count := 0.U
    }.otherwise {
      count := count + 1.U
    }
  }
}
