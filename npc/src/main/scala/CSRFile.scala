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
  val mcycle  = RegInit(0.U(32.W))
  val mcycleh = RegInit(0.U(32.W))
  val mvendorid = RegInit(0x79737978.U(32.W))
  val marchid = RegInit(26010036.U(32.W))

  io.rdata := 0.U

  when(io.ecall) {
    mepc     := io.wdata
    mcause   := 11.U
    io.rdata := mtvec
  }.otherwise {
    //写
    when(io.wen) {
      switch(io.addr) {
        is(0x341.U) { mepc := io.wdata }
        is(0x300.U) { mstatus := io.wdata }
        is(0x342.U) { mcause := io.wdata }
        is(0x305.U) { mtvec := io.wdata }
      }
    }
    //读
    switch(io.addr) {
      is(0x341.U) { io.rdata := mepc }
      is(0x300.U) { io.rdata := mstatus }
      is(0x342.U) { io.rdata := mcause }
      is(0x305.U) { io.rdata := mtvec }
      is(0xB00.U) { io.rdata := mcycle }
      is(0xB80.U) { io.rdata := mcycleh }
      is(0xF11.U) { io.rdata := mvendorid }
      is(0xF12.U) { io.rdata := marchid }
    }

  }

  mcycle := mcycle + 1.U
  when(mcycle === 0xffffffff.U) {
    mcycleh := mcycleh + 1.U
  }

}
