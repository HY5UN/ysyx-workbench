package top

import chisel3._
import chisel3.util._

class ExecutionUnit extends Module {
  val io = IO(new Bundle {
    val op1    = Input(UInt(32.W))
    val op2    = Input(UInt(32.W))
    val aluOp  = Input(UInt(4.W))
    val result = Output(UInt(32.W))

  })

  import ControlConstants._

  io.result := 0.U
  switch(io.aluOp) {
    is(ALU_ADD) { io.result := io.op1 + io.op2 } 
    is(ALU_SUB) { io.result := io.op1 - io.op2 } 
    is(ALU_XOR) { io.result := io.op1 ^ io.op2 }
    is(ALU_OR)  { io.result := io.op1 | io.op2 }
    is(ALU_AND) { io.result := io.op1 & io.op2 }
    is(ALU_LL)  { io.result := io.op1 << io.op2(4, 0) }
    is(ALU_RL)  { io.result := io.op1 >> io.op2(4, 0) }
    is(ALU_RA)  { io.result := (io.op1.asSInt >> io.op2(4, 0)).asUInt }
    is(ALU_LT)  { io.result := (io.op1.asSInt < io.op2.asSInt).asUInt }
    is(ALU_LTU) { io.result := (io.op1 < io.op2).asUInt }
    is(ALU_EQ)  { io.result := (io.op1 === io.op2).asUInt }
    is(ALU_NEQ) { io.result := (io.op1 =/= io.op2).asUInt }
    is(ALU_GE)  { io.result := (io.op1.asSInt >= io.op2.asSInt).asUInt }  
    is(ALU_GEU) { io.result := (io.op1 >= io.op2).asUInt }
  }

}
