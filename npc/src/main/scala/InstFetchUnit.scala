package top

import chisel3._
import chisel3.util._

class IFUMsg extends Bundle {
  val inst = UInt(32.W)
}

class InstFetchUnit extends ExtModule{
    val io =IO(new Bundle{
        val pc = Input(UInt(32.W))
        val out = Decoupled(new IFUMsg)
        
    })
}