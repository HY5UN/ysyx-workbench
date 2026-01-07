package lab
import chisel3._
import chisel3.util._

class PS2KeyboardRx extends Module {
  val io      = IO(new Bundle {
    val ps2clk  = Input(Bool())
    val ps2data = Input(Bool())

    val data  = Output(UInt(8.W))
    val ready = Output(Bool())
  })
  val dataReg = RegInit(0.U(8.W))
  io.data := dataReg
  val readyReg = RegInit(false.B)
  io.ready := readyReg

  val ps2ClkSync = RegInit(7.U(3.W))
  ps2ClkSync := Cat(ps2ClkSync(1, 0), io.ps2clk)

  val ps2Clk = ps2ClkSync(2) & ~ps2ClkSync(1)

  val bitCnt    = RegInit(0.U(4.W))
  val shiftReg  = RegInit(0.U(10.W))
  val receiving = RegInit(false.B)
  val start     = RegInit(false.B)

  when(ps2Clk) {
      readyReg  := false.B
    
    when(!receiving & io.ps2data === 0.B) {
      start := true.B

    }.elsewhen(start) {
      receiving := true.B
      bitCnt    := 0.U
      start     := false.B
    }.elsewhen(receiving) {
      bitCnt := bitCnt + 1.U

      shiftReg := Cat(io.ps2data, shiftReg(9, 1))
    }.elsewhen(bitCnt === 10.U) {
      receiving := false.B
      dataReg   := shiftReg(7, 0)
      val oddOk  = (shiftReg(7, 0).xorR ^ shiftReg(8)) === 1.B
      val stopOk = shiftReg(9) === 1.B
      readyReg := oddOk & stopOk
      bitCnt   := 0.U
    }
  }
}
