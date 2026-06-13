package top

import chisel3._
import chisel3.util._

import ControlConstants._



class ExecutionUnit extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new IDU2EXU))
    val out = Decoupled(new EXU2LSU)

    val rdata1 = Input(UInt(32.W))
    val rdata2 = Input(UInt(32.W))
    val csrRdata = Input(UInt(32.W))
  })
  val ctrl = io.in.bits.ctrl
  
  val alu = Module(new ALU())

  alu.io.op1 := Mux(ctrl.op1Sel === Op1Sel.RS1, io.rdata1, io.in.bits.pc)
  alu.io.op2 := MuxLookup(ctrl.op2Sel, io.rdata2)(
    Seq(
      Op2Sel.RS2 -> io.rdata2,
      Op2Sel.IMM -> io.in.bits.imm,
      Op2Sel.CSR -> io.csrRdata 
    )
  )
  alu.io.aluOp := ctrl.aluOp

  io.out.bits.result := alu.io.result
  io.out.bits.ctrl   := ctrl
  io.out.bits.rdata1 := io.rdata1
  io.out.bits.rdata2 := io.rdata2
  io.out.bits.pc := io.in.bits.pc
  io.out.bits.imm := io.in.bits.imm
  io.out.bits.rd := io.in.bits.rd

  io.out.valid     := io.in.valid
  io.in.ready      := io.out.ready

}

class ALU extends Module {
  val io = IO(new Bundle {
    val op1    = Input(UInt(32.W))
    val op2    = Input(UInt(32.W))
    val aluOp  = Input(UInt(4.W))
    val result = Output(UInt(32.W))
  })

  io.result := 0.U
  switch(io.aluOp) {
    is(AluOp.ADD) { io.result := io.op1 + io.op2 }
    is(AluOp.SUB) { io.result := io.op1 - io.op2 }
    is(AluOp.XOR) { io.result := io.op1 ^ io.op2 }
    is(AluOp.OR) { io.result := io.op1 | io.op2 }
    is(AluOp.AND) { io.result := io.op1 & io.op2 }
    is(AluOp.LL) { io.result := io.op1 << io.op2(4, 0) }
    is(AluOp.RL) { io.result := io.op1 >> io.op2(4, 0) }
    is(AluOp.RA) { io.result := (io.op1.asSInt >> io.op2(4, 0)).asUInt }
    is(AluOp.LT) { io.result := (io.op1.asSInt < io.op2.asSInt).asUInt }
    is(AluOp.LTU) { io.result := (io.op1 < io.op2).asUInt }
    is(AluOp.EQ) { io.result := (io.op1 === io.op2).asUInt }
    is(AluOp.NEQ) { io.result := (io.op1 =/= io.op2).asUInt }
    is(AluOp.GE) { io.result := (io.op1.asSInt >= io.op2.asSInt).asUInt }
    is(AluOp.GEU) { io.result := (io.op1 >= io.op2).asUInt }
  }

}
