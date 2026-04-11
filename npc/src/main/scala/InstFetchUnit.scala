package top

import chisel3._
import chisel3.util._

class IFUMsg extends Bundle {
  val inst = UInt(32.W)
}

class InstFetchUnitExt extends ExtModule{
    val io =IO(new Bundle{
        val pc = Input(UInt(32.W))
        val inst = Output(UInt(32.W))   
        
    })
}

class InstFetchUnit extends Module {
  val io = IO(new Bundle {
    val pc = Input(UInt(32.W))
    val out = Decoupled(new IFUMsg)
  })

  val ifuExt = Module(new InstFetchUnitExt())
    ifuExt.io.pc := io.pc
    io.out.bits.inst := ifuExt.io.inst
    io.out.valid := false.B

}