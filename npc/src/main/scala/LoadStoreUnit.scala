import chisel3._
import chisel3.util._

package top

class LoadStoreUnit extends Module {
  val io = IO(new Bundle {
    val addr = Input(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val rdata = Output(UInt(32.W))
    val wen = Input(Bool())
  })

  

}