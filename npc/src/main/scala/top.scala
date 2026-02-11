package top
import chisel3._
import chisel3.util._
import chisel3.probe.{force, forceInitial, read, release, releaseInitial, RWProbe, RWProbeValue}

class top extends Module {
  val io = IO(new Bundle {
    val inst   = Input(UInt(32.W))
    val pc     = Output(UInt(32.W))
    val to_mem = new MemIO
  })

  val pcReg = RegInit(0.U(32.W))
  pcReg := pcReg + 4.U
  io.pc := pcReg

  val idu = Module(new RV32EDecoder())
  dontTouch(idu.io)
  idu.io.inst := io.inst

  val reg = Module(new RegFile())

  reg.io.raddr1 := idu.io.rs1
  reg.io.raddr2 := idu.io.rs2
  reg.io.waddr  := idu.io.rd
  reg.io.wen    := idu.io.regWen

  import ControlConstants._

  val exu = Module(new ExecutionUnit())
  dontTouch(exu.io)
  exu.io.op1   := Mux(idu.io.op1Sel === OP1_RS1, reg.io.rdata1, io.pc)
  exu.io.op2   := Mux(idu.io.op2Sel === OP2_RS2, reg.io.rdata2, idu.io.imm)
  exu.io.aluOp := idu.io.aluOp

  reg.io.wdata := exu.io.result

  val lsu = Module(new LoadStoreUnit())
  lsu.io.addr  := exu.io.result
  lsu.io.wdata := reg.io.rdata2
  lsu.io.wen   := idu.io.memWen

  io.to_mem <> lsu.io.dmem

  reg.io.wdata := MuxLookup(
    idu.io.rdSel,
    reg.io.wdata,
    Seq(
      RD_ALU -> exu.io.result,
      RD_MEM -> lsu.io.rdata,
      RD_PC4 -> (io.pc + 4.U)
    )
  )

}
