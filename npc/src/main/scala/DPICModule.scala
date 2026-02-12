package top
import chisel3._
import chisel3.util._

class DPICModule extends ExtModule {
  val io = IO(new Bundle {
    val ebreak = Input(Bool())
  })

  
}