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
    val mret  = Input(Bool())
  })

  val mepc      = RegInit(0.U(32.W))
  val mstatus   = RegInit(0.U(32.W))
  val mcause    = RegInit(0.U(32.W))
  val mtvec     = RegInit(0.U(32.W))
  val mcycle    = RegInit(0.U(32.W))
  val mcycleh   = RegInit(0.U(32.W))
  val mvendorid = RegInit(0x79737978.U(32.W))
  val marchid   = RegInit(0x18ce1b4.U(32.W))

  io.rdata := 0.U

  when(io.ecall) {
    mepc     := io.wdata
    mcause   := 11.U
    io.rdata := mtvec
  }.otherwise when (io.mret) {
    io.rdata := mepc
    
  }.otherwise {
    // 写
    when(io.wen) {
      switch(io.addr) {
        is(0x341.U) { mepc := io.wdata }
        is(0x300.U) { mstatus := io.wdata }
        is(0x342.U) { mcause := io.wdata }
        is(0x305.U) { mtvec := io.wdata }
      }
    }
    // 读
    switch(io.addr) {
      is(0x341.U) { io.rdata := mepc }
      is(0x300.U) { io.rdata := mstatus }
      is(0x342.U) { io.rdata := mcause }
      is(0x305.U) { io.rdata := mtvec }
      is(0xb00.U) { io.rdata := mcycle }
      is(0xb80.U) { io.rdata := mcycleh }
      is(0xf11.U) { io.rdata := mvendorid }
      is(0xf12.U) { io.rdata := marchid }
    }

  }

  val time = RegInit(1.U(64.W))
  time := time + 1.U

  mcycle  := time(31, 0)
  mcycleh := time(63, 32)

}
