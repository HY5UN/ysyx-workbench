package top
import chisel3._
import chisel3.util._
import chisel3.util.random.LFSR


class RandomDelay(maxBits: Int = 4) extends Module {
  val io = IO(new Bundle {
    val trigger = Input(Bool())   // 触发延迟
    val ready   = Output(Bool())  // 延迟结束
  })

  val lfsr    = LFSR(8, io.trigger)   // 触发时取新随机值
  val cnt     = RegInit(0.U(maxBits.W))
  val active  = RegInit(false.B)

  when(io.trigger && !active) {
    cnt    := lfsr(maxBits - 1, 0)
    active := true.B
  }

  when(active) {
    when(cnt === 0.U) {
      active := false.B
      readyReg := true.B
    }.otherwise {
      cnt := cnt - 1.U
    }
  }

  val readyReg = RegInit(false.B)
  // readyReg := !active
  io.ready := readyReg

  // io.ready := !active
}
