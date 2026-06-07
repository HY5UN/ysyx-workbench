package top

import chisel3._
import chisel3.util._

class InstFetchUnit extends Module {
  val io = IO(new Bundle {
    val out   = Decoupled(new IFU2IDU)
    val in    = Flipped(Decoupled(new WBU2IFU))
    val memIO = new AXI4LiteIO
  })

  val tie0 = Module(new AXI4LiteTie0)
  tie0.io.s <> tie0.io.m
  tie0.io.m <> io.memIO

  val outInstReg = RegInit(0.U(32.W))
  val outValidReg = RegInit(false.B)
  val inReadyReg  = RegInit(false.B)

  val araddrReg  = RegInit("h80000000".U(32.W))
  val arvalidReg = RegInit(true.B)
  val rreadyReg  = RegInit(false.B)
  io.memIO.araddr  := araddrReg
  io.memIO.arvalid := arvalidReg
  io.memIO.rready  := rreadyReg

  object State extends ChiselEnum {
    val sIdle, sArWait, sRWait, sOut = Value
  }
  val state = RegInit(State.sArWait)
  switch(state) {
    is(State.sIdle) {
      when(io.in.fire) {
        araddrReg  := io.in.bits.nextPC
        arvalidReg := true.B
        inReadyReg := false.B
        state      := State.sArWait
      }
    }
    is(State.sArWait) {
      when(arvalidReg && io.memIO.arready) {
        arvalidReg := false.B
        rreadyReg  := true.B
        state      := State.sRWait
      }
    }
    is(State.sRWait) {
      when(io.memIO.rvalid && rreadyReg) {
        state       := State.sOut
        outInstReg  := io.memIO.rdata
        outValidReg := true.B
        rreadyReg   := false.B
      }
    }
    is(State.sOut) {
      when(io.out.fire) {
        state       := State.sIdle
        inReadyReg  := true.B
        outValidReg := false.B
      }
    }
  }

  io.out.valid := outValidReg
  io.in.ready  := inReadyReg

  io.out.bits.inst := outInstReg
  io.out.bits.pc   := araddrReg
}
