package top
import chisel3._
import chisel3.util._
import chisel3.probe.{force, forceInitial, read, release, releaseInitial, RWProbe, RWProbeValue}

import ControlConstants._

class ysyx_26010036 extends Module {
  val io = IO(new Bundle {
    val interrupt = Input(Bool())
    val master   = new AXI4IO
    val slave   = Flipped(new AXI4IO)
  })
  val mtie0 = Module(new AXI4MasterTie0())
  val stie0 = Module(new AXI4SlaveTie0())
  mtie0.io.m <> io.master
  io.slave <> stie0.io.s

  val ifu = Module(new InstFetchUnit())
  val idu = Module(new RV32EDecoder())
  val exu = Module(new ExecutionUnit())
  val lsu = Module(new LoadStoreUnit())
  val wbu = Module(new WriteBackUnit())
  StageConnect(ifu.io.out, idu.io.in)
  StageConnect(idu.io.out, exu.io.in)
  StageConnect(exu.io.out, lsu.io.in)
  StageConnect(lsu.io.out, wbu.io.in)
  StageConnect(wbu.io.out, ifu.io.in)

  val reg = Module(new RegFile())

  // Reg
  reg.io.raddr1 := idu.io.rs1 // idu阶段读取
  reg.io.raddr2 := idu.io.rs2
  exu.io.rdata1 := reg.io.rdata1
  exu.io.rdata2 := reg.io.rdata2
  reg.io.wen    := wbu.io.wen // wbu阶段写回
  reg.io.waddr  := wbu.io.rd
  reg.io.wdata  := wbu.io.wdata

  val csr = Module(new CSRFile())

  // CSR
  
  csr.io.addr     := idu.io.out.bits.imm// idu阶段解码与读取
  exu.io.csrRdata := csr.io.rdata
  csr.io.ecall    := wbu.io.ecall // wbu阶段写回
  csr.io.mret     := wbu.io.mret
  csr.io.wdata    := wbu.io.csrWdata            
  csr.io.wen      := wbu.io.csrWen
  wbu.io.csrRdata := csr.io.rdata 

  // mem
  // val mem = Module(new MemExt())
  val arb = Module(new MemArbiter())
  // val xbar = Module(new MemXbar())
  // val uart = Module(new UART())
  // val clint = Module(new CLINT())
  lsu.io.memIO <> arb.io.s1
  ifu.io.memIO <> arb.io.s0
  // mem.io.axi <> xbar.io.mRAM
  // uart.io.axi <> xbar.io.mUART
  // clint.io.axi <> xbar.io.mCLINT
  // xbar.io.s <> arb.io.m
  // mem.io.clock := clock
  // mem.io.reset := reset
  arb.io.m := 0.U.asTypeOf(arb.io.m)
  



  // dpic 控制
  val dpic = Module(new DPICModule())
  dpic.io.ebreak := idu.io.out.bits.ctrl.ebreak
  val difftest_step = RegInit(false.B) // 延迟一拍等待寄存器更新
  dpic.io.difftest_step := difftest_step
  difftest_step         := ifu.io.in.fire
  val nextPCReg = RegInit(0.U(32.W))
  nextPCReg := Mux(ifu.io.in.fire, wbu.io.out.bits.nextPC, nextPCReg)

}

object StageConnect {
  def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) = {
    val arch = "multi"
    // 为展示抽象的思想, 此处代码省略了若干细节
    if (arch == "single") {
      right <> left
    } else if (arch == "multi") { right <> left }
    // else if (arch == "pipeline") { right <> RegEnable(left, left.fire) }
    // else if (arch == "ooo") { right <> Queue(left, 16) }
  }
}
