package top
import chisel3._
import chisel3.util._
import chisel3.probe.{force, forceInitial, read, release, releaseInitial, RWProbe, RWProbeValue}

import ControlConstants._

class top extends Module {
  val io = IO(new Bundle {

    // 调试接口
    val pc     = Output(UInt(32.W))
    val inst   = Output(UInt(32.W))
    val allReg = Output(Vec(16, UInt(32.W)))
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

  // ebreak 控制
  val dpic = Module(new DPICModule())
  dpic.io.ebreak := idu.io.out.bits.ctrl.ebreak

  // 连接调试信息
  io.pc     := ifu.io.out.bits.pc
  io.inst   := ifu.io.out.bits.inst
  io.allReg := reg.io.regs
}

object StageConnect {
  def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) = {
    val arch = "single"
    // 为展示抽象的思想, 此处代码省略了若干细节
    if      (arch == "single")   { right.bits := left.bits }
    else if (arch == "multi")    { right <> left }
    else if (arch == "pipeline") { right <> RegEnable(left, left.fire) }
    else if (arch == "ooo")      { right <> Queue(left, 16) }
  }
}