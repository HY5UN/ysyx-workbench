package top
import chisel3._
import chisel3.util._

class DPICModule extends ExtModule {
  val io = IO(new Bundle {
    val clk = Input(Bool())
    val ebreak = Input(Bool())
    val difftest_step = Input(Bool())
    val gpr = Input(Vec(16, UInt(32.W)))
    val nextPC = Input(UInt(32.W))
    val pc = Input(UInt(32.W))
    val inst = Input(UInt(32.W))
    val instfetch = Input(Bool())
    val lsu_r = Input(Bool())
    val lsu_w = Input(Bool())
    val exu = Input(Bool())
    val inst_r = Input(Bool())
    val inst_i = Input(Bool())
    val inst_l = Input(Bool())
    val inst_s = Input(Bool())
    val inst_b = Input(Bool())
    val inst_u = Input(Bool())
    val inst_j = Input(Bool())
    val inst_csr = Input(Bool())
    val inst_sys = Input(Bool())
  })
 

  
}