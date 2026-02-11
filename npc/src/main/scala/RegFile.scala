package top

import chisel3._
import chisel3.util._


class RegFile extends Module {
  val io      = IO(new Bundle {
    val raddr1 = Input(UInt(5.W))
    val raddr2 = Input(UInt(5.W))
    val rdata1 = Output(UInt(32.W))
    val rdata2 = Output(UInt(32.W))
    val waddr  = Input(UInt(5.W))
    val wdata  = Input(UInt(32.W))
    val wen    = Input(Bool())
  })
  val regFile = Reg(Vec(32, UInt(32.W)))
  regFile(0) := 0.U
  io.rdata1  := Mux(io.raddr1 === 0.U, 0.U, regFile(io.raddr1))
  io.rdata2  := Mux(io.raddr2 === 0.U, 0.U, regFile(io.raddr2))
  when(io.wen && (io.waddr =/= 0.U)) {
    regFile(io.waddr) := io.wdata
  }

}
