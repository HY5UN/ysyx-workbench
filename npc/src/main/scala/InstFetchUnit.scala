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
    val nextPC = Input(UInt(32.W))
  })
  val pc = RegInit("h80000000".U(32.W))

  val ifu = Module(new InstFetchUnitExt())
  io.out.bits.inst := ifu.io.inst
  io.out.bits.pc   := pc
  io.out.valid     := true.B
  pc := io.nextPC
}
