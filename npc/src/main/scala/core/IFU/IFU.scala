package top

import chisel3._
import chisel3.util._

class IFU2IDU extends Bundle {
  val inst     = UInt(32.W)
  val pc       = UInt(32.W)
  val excValid = Bool()
  val excType  = ExceptionType()
  val pfm_tag  = UInt(8.W)
}

class IFU extends Module {
  val io = IO(new Bundle {
    val out           = Decoupled(new IFU2IDU)
    val axi           = new AXI4IO
    val flush         = Input(Bool())
    val nextPc        = Input(UInt(32.W))
    val pfm_miss      = Output(Bool())
    val pfm_if_begin  = Output(Bool())
    val pfm_if_finish = Output(Bool())
  })

  // val araddrReg = RegInit("h80000000".U(32.W))
  val araddrReg = RegInit("h30000000".U(32.W))
  object State extends ChiselEnum {
    val sInit, sIdle, sPcWait, sIWait, sOut = Value
  }
  val state = RegInit(State.sInit)
  val icache = Module(new ICache(cacheSizeB = 128, blockSizeB = 16, assoc = 2))
  icache.io.axi <> io.axi
  icache.io.ifu.pc     := araddrReg
  icache.io.ifu.fencei := false.B

  val flushReg  = RegEnable(io.flush, io.flush)
  val nextPcReg = RegEnable(io.nextPc, io.flush)

  io.out.bits.inst := icache.io.ifu.inst
  io.out.bits.pc   := araddrReg
  io.out.valid     := false.B

  val pfm_tagReg      = Reg(UInt(8.W))
  val pfm_ifFinishReg = RegInit(false.B)

  when(io.out.fire || flushReg || io.flush) {
    when(flushReg || io.flush) {
      flushReg   := false.B
      araddrReg  := Mux(io.flush, io.nextPc, nextPcReg)
      pfm_tagReg := pfm_tagReg + 1.U
    }.otherwise {
      araddrReg  := araddrReg + 4.U
      pfm_tagReg := pfm_tagReg + 1.U

    }
  }
  when(!(flushReg || io.flush)) {
    when(icache.io.ifu.instValid) {
      io.out.valid := true.B
    }
  }
  io.out.bits.excValid := false.B
  io.out.bits.excType  := ExceptionType.InstructionAccessFault
  when(icache.io.ifu.err) {
    io.out.bits.excValid := true.B
  }

  io.pfm_miss      := icache.io.miss
  io.pfm_if_begin  := state === State.sIdle || state === State.sInit
  io.pfm_if_finish := pfm_ifFinishReg

  io.out.bits.pfm_tag := pfm_tagReg
}
