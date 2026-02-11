package top
import chisel3._
import chisel3.util._

class MemIO extends Bundle {
  val wen   = Output(Bool())      // 写使能
  val addr  = Output(UInt(32.W))  // 地址 (LSU -> Mem)
  val len   = Output(UInt(2.W))   // 访问长度 (LSU -> Mem)
  val wdata = Output(UInt(32.W))  // 写数据 (LSU -> Mem)
  val rdata = Input(UInt(32.W))   // 读数据 (Mem -> LSU)
}

class LoadStoreUnit extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val rdata = Output(UInt(32.W))
    val wen = Input(Bool())
    val len = Input(UInt(2.W))
    val dmem = new MemIO
  })

  io.dmem.addr := io.addr
  io.dmem.wdata := io.wdata
  io.dmem.wen := io.wen
  io.dmem.len := io.len
  io.rdata := io.dmem.rdata

}