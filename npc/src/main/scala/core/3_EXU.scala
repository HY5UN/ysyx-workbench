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

  alu.io.op1 := Mux(ctrl.op1Sel === OP1_RS1, io.rdata1, io.in.bits.pc)
  alu.io.op2 := MuxLookup(ctrl.op2Sel, io.rdata2)(
    Seq(
      OP2_RS2 -> io.rdata2,
      OP2_IMM -> io.in.bits.imm,
      OP2_CSR -> io.csrRdata 
    )
  )
  alu.io.aluOp := ctrl.aluOp

  io.out.bits.result := alu.io.result
  io.out.bits.ctrl   := ctrl
  io.out.bits.rdata1 := io.rdata1
  io.out.bits.rdata2 := io.rdata2
  io.out.bits.pc := io.in.bits.pc
  io.out.bits.imm := io.in.bits.imm
  io.out.bits.csrRdata := io.csrRdata
  io.out.bits.rd := io.in.bits.rd

  io.out.valid     := io.in.valid
  io.in.ready      := true.B

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
    is(ALU_ADD) { io.result := io.op1 + io.op2 }
    is(ALU_SUB) { io.result := io.op1 - io.op2 }
    is(ALU_XOR) { io.result := io.op1 ^ io.op2 }
    is(ALU_OR) { io.result := io.op1 | io.op2 }
    is(ALU_AND) { io.result := io.op1 & io.op2 }
    is(ALU_LL) { io.result := io.op1 << io.op2(4, 0) }
    is(ALU_RL) { io.result := io.op1 >> io.op2(4, 0) }
    is(ALU_RA) { io.result := (io.op1.asSInt >> io.op2(4, 0)).asUInt }
    is(ALU_LT) { io.result := (io.op1.asSInt < io.op2.asSInt).asUInt }
    is(ALU_LTU) { io.result := (io.op1 < io.op2).asUInt }
    is(ALU_EQ) { io.result := (io.op1 === io.op2).asUInt }
    is(ALU_NEQ) { io.result := (io.op1 =/= io.op2).asUInt }
    is(ALU_GE) { io.result := (io.op1.asSInt >= io.op2.asSInt).asUInt }
    is(ALU_GEU) { io.result := (io.op1 >= io.op2).asUInt }
  }

}
