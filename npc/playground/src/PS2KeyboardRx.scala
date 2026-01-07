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
  val ps2ClkSync = RegInit(7.U(3.W))
  ps2ClkSync := Cat(ps2ClkSync(1, 0), io.ps2clk.asUInt)
  val sampling = ps2ClkSync(2) && !ps2ClkSync(1)

  val count  = RegInit(0.U(4.W))    // 0..10
  val bits   = RegInit(0.U(11.W))   // 存满 11 位
  val dataR  = RegInit(0.U(8.W))
  val readyR = RegInit(false.B)

  io.data  := dataR
  io.ready := readyR

  // 默认 ready 只打一拍
  readyR := false.B

  when(sampling) {
    // 在下降沿把当前 ps2data 塞进 bits(count)
    bits := bits.bitSet(count, io.ps2data)

    when(count === 10.U) {
      // 一帧收完：bits(0)=start, bits(8,1)=data(LSB first), bits(9)=parity, bits(10)=stop
      val startOk = bits(0) === 0.U
      val data    = bits(8, 1)
      val parity  = bits(9)
      val stopOk  = bits(10) === 1.U

      // odd parity：data xor parity == 1
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

