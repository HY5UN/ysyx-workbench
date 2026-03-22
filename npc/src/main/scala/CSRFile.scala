package top
import chisel3._
import chisel3.util._
class CSRFile extends Module {
  val io = IO(new Bundle {
    val addr  = Input(UInt(12.W))
    val rdata = Output(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val wen   = Input(Bool())

    val ecall = Input(Bool())
  })

  val mepc    = RegInit(0.U(32.W))
  val mstatus = RegInit(0.U(32.W))
  val mcause  = RegInit(0.U(32.W))
  val mtvec   = RegInit(0.U(32.W))

  io.rdata := 0.U

  when(io.ecall) {
    mepc     := io.wdata
    mcause   := 11.U
    io.rdata := mtvec
  }.otherwise {
    when(io.wen) {
      switch(io.addr) {
        is(0x341.U) { mepc := io.wdata }
        is(0x300.U) { mstatus := io.wdata }
        is(0x342.U) { mcause := io.wdata }
        is(0x305.U) { mtvec := io.wdata }
      }
    }

    switch(io.addr) {
      is(0x341.U) { io.rdata := mepc }
      is(0x300.U) { io.rdata := mstatus }
      is(0x342.U) { io.rdata := mcause }
      is(0x305.U) { io.rdata := mtvec }
    }

  }

}
