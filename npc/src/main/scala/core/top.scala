package top
import chisel3._
import chisel3.util._
import chisel3.probe.{force, forceInitial, read, release, releaseInitial, RWProbe, RWProbeValue}

import ControlConstants._

class top extends Module {
  val io = IO(new Bundle {

    //   // 调试接口
    val nextPC = Output(UInt(32.W))
    val pc     = Output(UInt(32.W))
    val inst   = Output(UInt(32.W))
    val reg    = Output(Vec(16, UInt(32.W)))
  })

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
  csr.io.ecall    := idu.io.out.bits.ctrl.ecall // idu阶段解码与读取
  csr.io.mret     := idu.io.out.bits.ctrl.mret
  csr.io.addr     := idu.io.out.bits.imm
  exu.io.csrRdata := csr.io.rdata
  csr.io.wdata    := wbu.io.csrWdata            // wbu阶段写回
  csr.io.wen      := wbu.io.csrWen

  // mem
  val mem = Module(new MemExt())
  val arb = Module(new MemArbiter())
  lsu.io.toMem <> arb.io.s1
  ifu.io.toMem <> arb.io.s0
  mem.io.axi <> arb.io.m
  mem.io.clock := clock
  mem.io.reset := reset

  // dpic 控制
  val dpic = Module(new DPICModule())
  dpic.io.ebreak := idu.io.out.bits.ctrl.ebreak
  val difftest_step = RegInit(false.B) // 延迟一拍等待寄存器更新
  dpic.io.difftest_step := difftest_step
  difftest_step         := wbu.io.out.valid

  // 连接调试信息
  io.pc     := ifu.io.out.bits.pc
  // io.nextPC := wbu.io.out.bits.nextPC
  io.nextPC := ifu.io.out.bits.pc
  io.inst   := ifu.io.out.bits.inst
  io.reg    := reg.io.regs
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
