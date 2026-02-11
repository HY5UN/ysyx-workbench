package top
import chisel3._
import chisel3.util._
import ControlConstants._


class top extends Module {
  val io = IO(new Bundle {
    val inst = Input(UInt(32.W))
    val pc = Output(UInt(32.W))
  })

  val pcReg = RegInit(0.U(32.W))
  pcReg := pcReg + 4.U
  io.pc := pcReg

  val idu = Module(new RV32EDecoder())
  idu.io.inst := io.inst
  
  val reg = Module(new Reg())
  reg.io.raddr1 := idu.io.rs1
  reg.io.raddr2 := idu.io.rs2
  reg.io.waddr := idu.io.rd
  reg.io.wen := idu.io.regWen


  val exu = Module(new ExecutionUnit())
  exu.io.op1 :=Mux(op1Sel === OP1_RS1, reg.io.rdata1, io.pc)
  exu.io.op2 :=Mux(op2Sel === OP2_RS2, reg.io.rdata2, idu.io.imm)
  exu.io.aluOp := idu.io.aluOp

  reg.io.wdata := exu.io.result

}
