package top
import chisel3._
import chisel3.util._

class LoadStoreUnit extends ExtModule {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val valid = Input(Bool())
    val addr  = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val wmask = Input(UInt(4.W))
    val wen   = Input(Bool())
    val rdata = Output(UInt(32.W))

  })
}
