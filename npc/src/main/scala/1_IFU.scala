package top

import chisel3._
import chisel3.util._

class InstFetchUnit extends Module {
  val io = IO(new Bundle {
    val out = Decoupled(new IFU2IDU)
    val in  = Flipped(Decoupled(new WBU2IFU))
  })
  object State extends ChiselEnum {
    val sIdle, sDelay, sWait = Value
  }

  val araddrReg  = RegInit("h80000000".U(32.W))
  val outInstReg   = RegInit(0.U(32.W))
  val state      = RegInit(State.sWait)
  val currPC     = RegInit("h80000000".U(32.W))
  val outValidReg = RegInit(false.B)
  val arvalidReg = RegInit(true.B)
  val rreadyReg  = RegInit(true.B)

  val reqValidDelay  = Module(new RandomDelay(4))
  val respReadyDelay = Module(new RandomDelay(3))
  reqValidDelay.io.trigger  := false.B
  respReadyDelay.io.trigger := false.B

  val ifuMem = Module(new InstFetchUnitExt())

  ifuMem.io.araddr  := araddrReg
  ifuMem.io.arvalid := arvalidReg
  ifuMem.io.rready  := rreadyReg
  ifuMem.io.clock   := clock
  ifuMem.io.reset   := reset

  switch(state) {
    // 空闲状态:已取出指令,等待新的有效地址
    is(State.sIdle) {
      when(io.in.valid) {
        araddrReg  := io.in.bits.nextPC
        arvalidReg := true.B
        outValidReg := false.B
        when(ifuMem.io.arready) { // 地址通道握手成功
          rreadyReg  := true.B
          state      := State.sWait
        }

      }
    }
    is(State.sDelay) {
      when(!arvalidReg) {
        arvalidReg := reqValidDelay.io.ready
      }
      when(!rreadyReg) {
        rreadyReg := respReadyDelay.io.ready
      }
      when(arvalidReg && rreadyReg) {
        state := State.sWait
      }
    }
    // 等待状态:等待指令返回,准备输出
    is(State.sWait) {
      arvalidReg := false.B
      when(ifuMem.io.rvalid){// 数据通道握手成功
        state := State.sIdle
        outInstReg:= ifuMem.io.rdata
        outValidReg := true.B
        rreadyReg  := false.B
      }

    }

  }
  io.out.valid := outValidReg
  io.in.ready  := state === State.sIdle

  io.out.bits.inst := outInstReg
  io.out.bits.pc   := araddrReg
}

class InstFetchUnitExt extends ExtModule {
  val io = IO(new Bundle {

    val clock = Input(Clock())
    val reset = Input(Bool())

    val araddr = Input(UInt(32.W))
    val arvalid = Input(Bool())
    val arready = Output(Bool())

    val rdata = Output(UInt(32.W))
    val rresp = Output(UInt(2.W))
    val rvalid = Output(Bool())
    val rready = Input(Bool())

  })
}
