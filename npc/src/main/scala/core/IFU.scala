package top

import chisel3._
import chisel3.util._

class InstFetchUnit extends Module {
  val io = IO(new Bundle {
    val out = Decoupled(new IFU2IDU)
    val in  = Flipped(Decoupled(new WBU2IFU))
    val axi = new AXI4IO
  })

  val axiTie0m = Module(new AXI4MasterTie0)
  axiTie0m.io.m <> io.axi
  io.axi.arsize  := "b010".U // 取指固定32bit = 4字节
  io.axi.arburst := "b01".U  // INCR

  val outInstReg = RegInit(0.U(32.W))
  val outPcReg   = RegInit(0.U(32.W))

  // val araddrReg  = RegInit("h80000000".U(32.W))
  val araddrReg = RegInit("h20000000".U(32.W))
  val arvalidReg = RegInit(false.B)
  val rreadyReg  = RegInit(false.B)
  io.axi.araddr  := araddrReg
  io.axi.arvalid := arvalidReg
  io.axi.rready  := rreadyReg

  object State extends ChiselEnum {
    val sInit, sIdle, sArWait, sRWait, sOut = Value
  }
  val state = RegInit(State.sInit)
  switch(state) {
    is(State.sInit){
      arvalidReg := true.B
      state      := State.sArWait
    }
    is(State.sIdle) {
      when(io.in.fire) {
        araddrReg  := io.in.bits.nextPC
        arvalidReg := true.B
        state      := State.sArWait
      }
    }
    is(State.sArWait) {
      when(arvalidReg && io.axi.arready) {
        arvalidReg := false.B
        rreadyReg  := true.B
        state      := State.sRWait
      }
    }
    is(State.sRWait) {
      when(io.axi.rvalid && rreadyReg) {
        state      := State.sOut
        outInstReg := io.axi.rdata
        rreadyReg  := false.B
        outPcReg   := araddrReg
        when(io.axi.rresp(1)) {
          outPcReg := 0.U
        }
      }
    }
    is(State.sOut) {
      when(io.out.fire) {
        state := State.sIdle
      }
    }
  }

  io.out.valid := state === State.sOut
  io.in.ready  := state === State.sIdle

  io.out.bits.inst := outInstReg
  io.out.bits.pc   := outPcReg
}
