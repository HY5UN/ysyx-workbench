package top
import chisel3._
import chisel3.util._

class LoadStoreUnit extends ExtModule {
  val clock = IO(Input(Clock()))
  val addr  = IO(Input(UInt(32.W)))
  val wdata = IO(Input(UInt(32.W)))
  val rdata = IO(Output(UInt(32.W)))
  val wen   = IO(Input(Bool()))

}
