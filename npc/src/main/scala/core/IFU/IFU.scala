package top

import chisel3._
import chisel3.util._

class IFU2IDU extends Bundle {
  val inst = UInt(32.W)
  val pc   = UInt(32.W)
}

class IFU extends Module {
  val io         = IO(new Bundle {
    val out         = Decoupled(new IFU2IDU)
    val in          = Flipped(Decoupled(new WBU2IFU))
    val axi         = new AXI4IO
    val miss        = Output(Bool())
    val branchTaken = Output(Bool())
  })
  val outInstReg = RegInit(0.U(32.W))
  val outPcReg   = RegInit(0.U(32.W))

  val araddrReg = RegInit("h80000000".U(32.W))
  // val araddrReg = RegInit("h30000000".U(32.W))
  object State extends ChiselEnum {
    val sInit, sIdle, sPcWait, sIWait, sOut = Value
  }
  val state = RegInit(State.sInit)
  val icache = Module(new ICache(cacheSizeB = 128, blockSizeB = 16, assoc = 2))
  icache.io.axi <> io.axi
  icache.io.ifu.pc      := araddrReg
  icache.io.ifu.pcValid := state === State.sPcWait
  icache.io.ifu.fencei  := false.B

  val inReg = RegEnable(io.in.bits, io.in.valid)
  val inBranchTaken =  (io.in.bits.branchTaken && io.in.valid)
  io.branchTaken := inReg.branchTaken || inBranchTaken
  switch(state) {
    is(State.sInit) {
      state := State.sPcWait
    }
    is(State.sIdle) {
      when(inReg.branchTaken) {
        inReg.branchTaken:=false.B
        araddrReg := inReg.nextPC
      }.otherwise {
        araddrReg := araddrReg + 4.U
      }
      state := State.sPcWait
    }
    is(State.sPcWait) {
      when(icache.io.ifu.instValid) {
        state      := State.sOut
        outInstReg := icache.io.ifu.inst
        outPcReg   := araddrReg
      }
    }
    is(State.sOut) {
      when(io.out.fire || (inReg.branchTaken || inBranchTaken )) {
        state := State.sIdle
      }
    }
  }
  io.miss        := icache.io.miss

  io.out.valid := state === State.sOut && !(inReg.branchTaken || inBranchTaken )
  io.in.ready  := state === State.sIdle

  io.out.bits.inst := outInstReg
  io.out.bits.pc   := outPcReg
}
