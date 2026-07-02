package top

import chisel3._
import chisel3.util._

class IFU2IDU extends Bundle {
  val inst     = UInt(32.W)
  val pc       = UInt(32.W)
  val excValid = Bool()
  val excType  = ExceptionType()
  val tag     = UInt(8.W)
}

class IFU extends Module {
  val io         = IO(new Bundle {
    val out    = Decoupled(new IFU2IDU)
    val axi    = new AXI4IO
    val flush  = Input(Bool())
    val nextPc = Input(UInt(32.W))
    val pfm_miss   = Output(Bool())
    val pfm_if_begin = Output(Bool())
    val pfm_if_finish = Output(Bool())
  })
  

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

  val flushReg  = RegEnable(io.flush, io.flush)
  val nextPcReg = RegEnable(io.nextPc, io.flush)

  val excTypeReg = Reg(ExceptionType())
  val excValidReg   = RegInit(false.B)
val outInstReg = RegInit(0.U(32.W))
  val outPcReg   = RegInit(0.U(32.W))
  val tagReg = Reg(UInt(8.W))
  switch(state) {
    is(State.sInit) {
      state := State.sPcWait
    }
    is(State.sIdle) {
      when(flushReg || io.flush) {
        flushReg  := false.B
        araddrReg := Mux(io.flush, io.nextPc, nextPcReg)
      }.otherwise {
        araddrReg := araddrReg + 4.U
      }
      state := State.sPcWait
    }
    is(State.sPcWait) {
      when(araddrReg(1, 0) =/= 0.U) {
        excTypeReg:= ExceptionType.InstructionAddressMisaligned
        excValidReg:=true.B
        icache.io.ifu.pcValid:= false.B
        state:= State.sOut
      }.elsewhen(icache.io.ifu.instValid) {
        state      := State.sOut
        outInstReg := icache.io.ifu.inst
        outPcReg   := araddrReg
        when(icache.io.ifu.err){
          excTypeReg:= ExceptionType.InstructionAccessFault
          excValidReg:= true.B
        }
      }
    }
    is(State.sOut) {
      when(io.out.fire || (flushReg || io.flush)) {
        state := State.sIdle
        excValidReg:=false.B
        tagReg := tagReg + 1.U
      }
    }
  }
  io.pfm_miss := icache.io.miss
  io.pfm_if_begin:= state=== State.sIdle || state===State.sInit
  io.pfm_if_finish:= state=== State.sOut

  io.out.bits.excValid := excValidReg
  io.out.bits.excType  := excTypeReg

  io.out.valid := state === State.sOut && !(flushReg || io.flush)

  io.out.bits.inst := outInstReg
  io.out.bits.pc   := outPcReg
  io.out.bits.tag  := tagReg
}
