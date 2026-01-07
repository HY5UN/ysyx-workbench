package lab
import chisel3._
import chisel3.util._

class PS2KeyboardRx extends Module {
  val io = IO(new Bundle {
    val ps2clk  = Input(Bool())
    val ps2data = Input(Bool())

    val data = output(UInt(8.W))
    val ready = output(Bool())
  })

  val ps2ClkSync = RegInit(1.U(3.W))
  ps2ClkSync := Cat(ps2ClkSync(1, 0), io.ps2clk)

  val ps2Clk = ps2ClkSync(2) & ~ps2ClkSync(1)

  val bitCnt    = RegInit(0.U(4.W))
  val shiftReg  = RegInit(0.U(10.W))
  val receiving = RegInit(false.B)

  when(ps2Clk) {
    when(bitCnt === 10.U) {
        receiving := false.B
        io.data   := shiftReg(7, 0)
        val oddOk =shiftReg(8,0).xorR
				val stopOk= shiftReg(9)===1.B
				io.ready  := oddOk & stopOk
				bitCnt    := 0.U
    }
    when(!receiving & io.ps2data === 0.B) {
      receiving := true.B
      bitCnt    := 0.U
    }.elsewhen(receiving) {
      bitCnt := bitCnt + 1.U
      
    	shiftReg := Cat(io.ps2data, shiftReg(9, 1))
      }
    }
  }

