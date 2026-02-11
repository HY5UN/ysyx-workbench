package top

import chisel3._
import chisel3.util._


object ControlConstants {
  val ALU_ADD = "b0000".U

	// opSel
	val OP1_RS1 = "b0".U
	val OP1_PC = "b1".U
	val OP2_RS2 = "b0".U
	val OP2_IMM = "b1".U
}

class RV32EDecoder extends Module {
  val io     = IO(new Bundle {
    val inst = Input(UInt(32.W))

    val rs1 = Output(UInt(5.W))
    val rs2 = Output(UInt(5.W))
    val rd  = Output(UInt(5.W))
    val imm = Output(UInt(32.W))

    val aluOp  = Output(UInt(4.W))
		val op1Sel = Output(UInt(1.W))
		val op2Sel = Output(UInt(1.W))
    val regWen = Output(Bool())

  })
  val opcode = io.inst(6, 0)
  val rd     = io.inst(11, 7)
  val funct3 = io.inst(14, 12)
  val rs1    = io.inst(19, 15)
  val rs2    = io.inst(24, 20)
  val funct7 = io.inst(31, 25)

  val immI = Wire(UInt(32.W))
  val immS = Wire(UInt(32.W))
  val immB = Wire(UInt(32.W))
  val immU = Wire(UInt(32.W))
  val immJ = Wire(UInt(32.W))

  immI := io.inst(31, 20).asSInt.asUInt
  immS := Cat(io.inst(31, 25), io.inst(11, 7)).asSInt.asUInt
  immB := Cat(io.inst(31), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W)).asSInt.asUInt
  immU := Cat(io.inst(31, 12), 0.U(12.W)).asSInt.asUInt
  immJ := Cat(io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W)).asSInt.asUInt

  // I-type
  val ADDI = BitPat("b?????????????????000?????0010011")

	io.rs1 := rs1
	io.rs2 := rs2
	io.rd := rd
	io.regWen := false.B
	io.aluOp := 0.U
	io.op1Sel := 0.U
	io.op2Sel := 0.U

  // switch(io.inst) {
  //   is(ADDI) {
  //     io.imm := immI
	// 		io.aluOp := ALU_ADD
	// 		io.regWen := true.B
	// 		io.op1Sel := OP1_RS1
	// 		io.op2Sel := OP2_IMM
  //   }
  // }
  import ControlConstants._
  when(io.inst === ADDI) {
    io.imm := immI
    io.aluOp := ALU_ADD 
    io.regWen := true.B
    io.op1Sel := OP1_RS1
    io.op2Sel := OP2_IMM
  } 
}
