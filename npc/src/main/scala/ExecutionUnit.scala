import chisel3._
import chisel3.util._
package top

class ExecutionUnit extends Module {
  val io = IO(new Bundle {
    val op1    = Input(UInt(32.W))
    val op2    = Input(UInt(32.W))
    val aluOp  = Input(UInt(4.W))
    val result = Output(UInt(32.W))

  })

  import ControlConstants._

  switch(io.aluOp) {
    is(ALU_ADD) { io.result := io.op1 + io.op2 } // ADD
  }

}
