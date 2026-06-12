package top
import chisel3._
import chisel3.util._

class DPICModule extends ExtModule {
  val io = IO(new Bundle {
    val ebreak = Input(Bool())
    val difftest_step = Input(Bool())
    val gpr = Input(Vec(16, UInt(32.W)))
    val nextPC = Input(UInt(32.W))
    val pc = Input(UInt(32.W))
    val inst = Input(UInt(32.W))
  })

  
}