package top
import chisel3._
import chisel3.util._
import chisel3.probe.{force, forceInitial, read, release, releaseInitial, RWProbe, RWProbeValue}

class top extends Module {
  val io = IO(new Bundle {

    val allReg =Output(Vec(32, UInt(32.W)))
  })

  val pcReg = RegInit(0.U(32.W))
  val ifu = Module(new InstFetchUnit())
  ifu.io.pc:= pcReg


  val idu = Module(new RV32EDecoder())
  idu.io.inst := ifu.io.inst

  val reg = Module(new RegFile())

  reg.io.raddr1 := idu.io.rs1
  reg.io.raddr2 := idu.io.rs2
  reg.io.waddr  := idu.io.rd
  reg.io.wen    := idu.io.regWen

  import ControlConstants._

  val exu = Module(new ExecutionUnit())
  exu.io.op1   := Mux(idu.io.op1Sel === OP1_RS1, reg.io.rdata1, pcReg)
  exu.io.op2   := Mux(idu.io.op2Sel === OP2_RS2, reg.io.rdata2, idu.io.imm)
  exu.io.aluOp := idu.io.aluOp

  reg.io.wdata := exu.io.result

  val lsu = Module(new LoadStoreUnit())
  lsu.io.addr  := exu.io.result
  lsu.io.wdata := reg.io.rdata2
  lsu.io.wen   := idu.io.memWen
  lsu.io.clock  := clock

  //写入rd
  reg.io.wdata := MuxLookup(idu.io.rdSel, exu.io.result)(
    Seq(
      RD_ALU -> exu.io.result,
      RD_MEM -> lsu.io.rdata,
      RD_PC4 -> (pcReg + 4.U)
    )
  )

  //更新pc
  pcReg := MuxLookup(idu.io.pcSel, pcReg + 4.U)(
    Seq(
      PC_4    -> (pcReg + 4.U),
      PC_ALU  -> exu.io.result,
      PC_ALU1 -> (exu.io.result & "hfffffffe".U)
    )
  )

  io.allReg := reg.io.regs
}