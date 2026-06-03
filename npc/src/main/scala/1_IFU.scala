package top

import chisel3._
import chisel3.util._

class InstFetchUnit extends Module {
  val io = IO(new Bundle {
    val out = Decoupled(new IFU2IDU)
    val in  = Flipped(Decoupled(new WBU2IFU))
  })
  object State extends ChiselEnum {
    val sIdle, sWait = Value
  }

  val pc        = RegInit("h80000000".U(32.W))
  val ifuRdata  = RegInit(0.U(32.W))
  val state     = RegInit(State.sWait)
  val currPC    = RegInit("h80000000".U(32.W))
  val reqValid  = RegInit(true.B)
  val respReady = RegInit(true.B)

  val ifu = Module(new InstFetchUnitExt())
  ifu.io.pc        := pc
  ifu.io.clock     := clock
  ifu.io.reset     := reset
  ifu.io.reqValid  := reqValid
  ifu.io.respReady := respReady

  switch(state) {
    // 空闲状态:已取出指令,等待新的有效地址
    is(State.sIdle) {
      when(io.in.valid) {
        state     := State.sWait
        pc        := io.in.bits.nextPC
        respReady := true.B
        reqValid  := true.B
      }
    }
    // 等待状态:等待指令返回,准备输出
    is(State.sWait) {
      reqValid := false.B
      when(ifu.io.reqReady) {
        when(ifu.io.respValid) {
          state     := State.sIdle
          ifuRdata  := ifu.io.inst
          currPC    := pc
          respReady := false.B
        }
      }
    }

  }
  io.out.valid := state === State.sIdle
  io.in.ready  := state === State.sIdle

  io.out.bits.inst := ifuRdata
  io.out.bits.pc   := currPC
}

class InstFetchUnitExt extends ExtModule {
  val io = IO(new Bundle {
    val pc        = Input(UInt(32.W))
    val inst      = Output(UInt(32.W))
    val clock     = Input(Clock())
    val reset     = Input(Bool())
    val reqValid  = Input(Bool())  // 地址信号有效
    val reqReady  = Output(Bool()) // 地址能被存储器成功接收
    val respValid = Output(Bool()) // 数据有效
    val respReady = Input(Bool())  // 数据能被cpu成功接收
  })
}
