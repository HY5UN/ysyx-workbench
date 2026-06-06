package top

import chisel3._
import chisel3.util._

class InstFetchUnit extends Module {
  val io = IO(new Bundle {
    val out   = Decoupled(new IFU2IDU)
    val in    = Flipped(Decoupled(new WBU2IFU))
    val memIO = new AXI4LiteIO
  })
  object State extends ChiselEnum {
    val sIdle, sDelay, sWait = Value
  }

  val araddrReg   = RegInit("h80000000".U(32.W))
  val outInstReg  = RegInit(0.U(32.W))
  val state       = RegInit(State.sIdle)
  val currPC      = RegInit("h80000000".U(32.W))
  val outValidReg = RegInit(false.B)
  val arvalidReg  = RegInit(true.B)
  val rreadyReg   = RegInit(true.B)

  io.memIO.araddr  := araddrReg
  io.memIO.arvalid := arvalidReg
  io.memIO.rready  := rreadyReg

  // 写通道不使用，接 0
  io.memIO.awaddr  := 0.U
  io.memIO.awvalid := false.B
  io.memIO.wdata   := 0.U
  io.memIO.wstrb   := 0.U
  io.memIO.wvalid  := false.B
  io.memIO.bready  := false.B

  switch(state) {
    // 空闲状态:已取出指令,等待新的有效地址
    is(State.sIdle) {
      when(io.in.valid) {
        araddrReg   := io.in.bits.nextPC
        arvalidReg  := true.B
        outValidReg := false.B
      }
      when(io.memIO.arready&& (arvalidReg||io.in.valid)) { // 地址通道握手成功
        rreadyReg := true.B
        state     := State.sWait
      }
    }
    // 等待状态:等待指令返回,准备输出
    is(State.sWait) {
      arvalidReg := false.B
      when(io.memIO.rvalid&&rreadyReg) { // 数据通道握手成功
        state       := State.sIdle
        outInstReg  := io.memIO.rdata
        outValidReg := true.B
        rreadyReg   := false.B
      }
    }
  }

  // 加入随机延迟
  // val arvalidDelay = Module(new RandomDelay(4))
  // val rreadyDelay  = Module(new RandomDelay(3))
  // arvalidDelay.io.trigger := false.B
  // rreadyDelay.io.trigger  := false.B

  // switch(state) {
  //   is(State.sIdle) {
  //     when(io.in.valid) {
  //       araddrReg               := io.in.bits.nextPC
  //       outValidReg             := false.B
  //       arvalidDelay.io.trigger := true.B
  //     }
  //     arvalidReg := arvalidDelay.io.ready || arvalidReg
  //     when(io.memIO.arready && arvalidReg){//读请求握手
  //       rreadyDelay.io.trigger := true.B
  //     }

  //     when(!rreadyReg) {
  //       when(rreadyDelay.io.ready) {
  //         rreadyReg := true.B
  //         state     := State.sWait
  //       }
  //     }
  //   }
  //   is(State.sWait) {
  //     arvalidReg := false.B
  //     when(io.memIO.rvalid) { // 读响应握手
  //       state       := State.sIdle
  //       outInstReg  := io.memIO.rdata
  //       outValidReg := true.B
  //       rreadyReg   := false.B
  //     }
  //   }
  // }

  io.out.valid := outValidReg
  io.in.ready  := state === State.sIdle

  io.out.bits.inst := outInstReg
  io.out.bits.pc   := araddrReg
}
