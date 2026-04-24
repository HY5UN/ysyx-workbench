package top

import chisel3._
import chisel3.util._

class InstFetchUnitExt extends ExtModule {
  val io = IO(new Bundle {
    val pc   = Input(UInt(32.W))
    val inst = Output(UInt(32.W))

  })
}

class InstFetchUnit extends Module {
  val io = IO(new Bundle {
    val out = Decoupled(new IFU2IDU)
    val in  = Flipped(Decoupled(new WBU2IFU))
  })
  object State extends ChiselEnum {
    val sIdle, sWait = Value
  }

  val pc       = RegInit("h80000000".U(32.W))
  val ifuRdata = RegInit(0.U(32.W))
  val state    = RegInit(State.sIdle)
  val ifuRaddr = RegInit("h80000000".U(32.W))


  val ifu = Module(new InstFetchUnitExt())
  ifu.io.pc := ifuRaddr

  switch(state) {
    // 空闲状态:已取出指令,等待新的有效地址
    is(State.sIdle) {
      when(io.in.valid||pc === "h80000000".U) {
        state := State.sWait
        ifuRaddr:=pc
        pc := io.in.bits.nextPC
      }
    }
    // 等待状态:等待指令返回,准备输出
    is(State.sWait) {
      state    := State.sIdle
      ifuRdata := ifu.io.inst
    }

  }
  io.out.valid := state === State.sIdle
  io.in.ready  := state === State.sIdle

  io.out.bits.inst := ifuRdata
  io.out.bits.pc   := ifuRaddr
}
