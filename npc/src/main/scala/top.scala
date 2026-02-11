package top
import chisel3._
import chisel3.util._
import chisel3.probe.{RWProbe, RWProbeValue, force, forceInitial, read, release, releaseInitial}

class top extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
    val pc   = Output(UInt(32.W))
  })

  val pcReg = RegInit(0.U(32.W))
  pcReg := pcReg + 4.U
  io.pc := pcReg

  val idu = Module(new RV32EDecoder())
  dontTouch(idu)
  idu.io.inst := io.inst

  val reg = Module(new RegFile())

  reg.io.raddr1 := idu.io.rs1
  reg.io.raddr2 := idu.io.rs2
  reg.io.waddr  := idu.io.rd
  reg.io.wen    := idu.io.regWen

  import ControlConstants._

  val exu = Module(new ExecutionUnit())
  dontTouch(exu)
  exu.io.op1   := Mux(idu.io.op1Sel === OP1_RS1, reg.io.rdata1, io.pc)
  exu.io.op2   := Mux(idu.io.op2Sel === OP2_RS2, reg.io.rdata2, idu.io.imm)
  exu.io.aluOp := idu.io.aluOp

  reg.io.wdata := exu.io.result

}
