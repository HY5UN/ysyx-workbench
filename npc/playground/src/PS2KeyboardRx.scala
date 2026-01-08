package lab
import chisel3._
import chisel3.util._

class PS2KeyboardRx extends Module {
  val io = IO(new Bundle {

    val ps2clk     = Input(Bool())
    val ps2data    = Input(Bool())
    val nextdata_n = Input(Bool())
    val data       = Output(UInt(8.W))
    val ready      = Output(Bool())
    val overflow   = Output(Bool())

  })

  val readyReg    = RegInit(false.B)
  val overflowReg = RegInit(false.B)

  val ps2ClkSync = RegInit(7.U(3.W))
  ps2ClkSync := Cat(ps2ClkSync(1, 0), io.ps2clk.asUInt)
  val sampling = ps2ClkSync(2) && !ps2ClkSync(1)

  val fifo   = Reg(Vec(8, UInt(8.W)))
  val wPtr   = RegInit(0.U(3.W))
  val rPtr   = RegInit(0.U(3.W))
  val count  = RegInit(0.U(4.W))
  val buffer = RegInit(0.U(10.W))

  when(!io.nextdata_n && readyReg) {
    rPtr := rPtr + 1.U
    when(rPtr+1.U === wPtr) {
      readyReg := false.B
    }
  }

  when(sampling) {
    when(count === 10.U) {
      val startBit = buffer(0)
      val stopBit  = io.ps2data

      when(
        startBit === 0.U &&
          buffer(9, 1).xorR &&
          stopBit === 1.U
      ) {
        fifo(wPtr)  := buffer(8, 1)
        wPtr        := wPtr + 1.U
        overflowReg := overflowReg || (wPtr + 1.U === rPtr)
        readyReg    := true.B
      }
      count := 0.U
    }.otherwise {
      buffer := buffer.bitSet(count, io.ps2data)
      count         := count + 1.U
    }
  }

  io.data     := fifo(rPtr)
  io.ready    := readyReg
  io.overflow := overflowReg

}
