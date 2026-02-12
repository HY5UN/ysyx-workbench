package top

import chisel3._
import chisel3.util._

object ControlConstants {

  // aluOp
  val ALU_ADD = "b0000".U

  // opSel
  val OP1_RS1 = "b0".U
  val OP1_PC  = "b1".U
  val OP2_RS2 = "b0".U
  val OP2_IMM = "b1".U

  // rdSel
  val RD_ALU = "b00".U
  val RD_MEM = "b01".U
  val RD_PC4 = "b10".U
  val RD_IMM = "b11".U

  // memLen
  val LEN_BYTE = "b00".U
  val LEN_HALF = "b01".U
  val LEN_WORD = "b10".U

  // pcSel
  val PC_4    = "b00".U
  val PC_ALU  = "b01".U
  val PC_ALU1 = "b10".U // 需要将计算结果低位清0

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
    val rdSel  = Output(UInt(2.W))
    val regWen = Output(Bool())
    val memWen = Output(Bool())
    val memLen = Output(UInt(2.W))
    val pcSel  = Output(UInt(2.W))

    val ebreak = Output(Bool())

  })
  val opcode = io.inst(6, 0)
  val rd     = io.inst(11, 7)
  val funct3 = io.inst(14, 12)
  val rs1    = io.inst(19, 15)
  val rs2    = io.inst(24, 20)
  val funct7 = io.inst(31, 25)

  // 定义 32 位的线网
  val immI = Wire(UInt(32.W))
  val immS = Wire(UInt(32.W))
  val immB = Wire(UInt(32.W))
  val immU = Wire(UInt(32.W))
  val immJ = Wire(UInt(32.W))

  // 1. 先转为 SInt
  // 2. 使用 .pad(32) 进行符号扩展 (Sign Extension)
  // 3. 最后转回 UInt (如果你的数据通路全是 UInt)
  
  // I-Type: 12-bit -> 32-bit
  immI := io.inst(31, 20).asSInt.pad(32).asUInt

  // S-Type: 12-bit -> 32-bit
  immS := Cat(io.inst(31, 25), io.inst(11, 7)).asSInt.pad(32).asUInt

  // B-Type: 13-bit (带末尾0) -> 32-bit
  immB := Cat(io.inst(31), io.inst(7), io.inst(30, 25), io.inst(11, 8), 0.U(1.W)).asSInt.pad(32).asUInt

  // U-Type: 已经是 32-bit 了，不需要扩展，但为了统一格式写上也无妨
  // 注意：U-Type 本质是高位填充，逻辑上不需要"符号扩展"，直接赋值即可，但 pad(32) 对 32bit 数是无操作，所以也没错。
  immU := Cat(io.inst(31, 12), 0.U(12.W)).asSInt.pad(32).asUInt

  // J-Type: 21-bit (带末尾0) -> 32-bit
  immJ := Cat(io.inst(31), io.inst(19, 12), io.inst(20), io.inst(30, 21), 0.U(1.W)).asSInt.pad(32).asUInt

  // R-type
  val ADD    = BitPat("b0000000_?????_?????_000_?????_0110011")
  // U
  val LUI    = BitPat("b???????_?????_?????_???_?????_0110111")
  // I-type
  val ADDI   = BitPat("b???????_?????_?????_000_?????_0010011")
  val JALR   = BitPat("b???????_?????_?????_000_?????_1100111")
  val LW     = BitPat("b???????_?????_?????_010_?????_0000011")
  val LBU    = BitPat("b???????_?????_?????_100_?????_0000011")
  // S-type
  val SW     = BitPat("b???????_?????_?????_010_?????_0100011")
  val SB     = BitPat("b???????_?????_?????_000_?????_0100011")
  // ebreak
  val EBREAK = BitPat("b0000000_00001_00000_000_00000_1110011")

  io.rs1 := rs1
  io.rs2 := rs2
  io.rd  := rd

  io.regWen := false.B
  io.memWen := false.B
  io.aluOp  := 0.U
  io.op1Sel := 0.U
  io.op2Sel := 0.U
  io.rdSel  := 0.U
  io.imm    := 0.U
  io.memLen := 0.U
  io.pcSel  := 0.U
  io.ebreak := false.B

  import ControlConstants._
  when(io.inst === ADDI) {
    io.imm    := immI
    io.aluOp  := ALU_ADD
    io.regWen := true.B
    io.op1Sel := OP1_RS1
    io.op2Sel := OP2_IMM
    io.rdSel  := RD_ALU
  }
    .elsewhen(io.inst === JALR) {
      io.imm    := immI
      io.aluOp  := ALU_ADD
      io.regWen := true.B
      io.op1Sel := OP1_RS1
      io.op2Sel := OP2_IMM
      io.rdSel  := RD_PC4
      io.pcSel  := PC_ALU1
    }
    .elsewhen(io.inst === EBREAK) {
      io.pcSel  := PC_4
      io.ebreak := true.B
    }
    .elsewhen(io.inst === ADD) {
      io.aluOp  := ALU_ADD
      io.regWen := true.B
      io.op1Sel := OP1_RS1
      io.op2Sel := OP2_RS2
      io.rdSel  := RD_ALU
    }
    .elsewhen(io.inst === LW) {
      io.regWen := true.B
      io.rdSel  := RD_MEM
      io.op1Sel := OP1_RS1
      io.op2Sel := OP2_IMM
      io.imm    := immI
      io.aluOp  := ALU_ADD
      io.memLen := LEN_WORD
    }
    .elsewhen(io.inst === LBU) {
      io.regWen := true.B
      io.rdSel  := RD_MEM
      io.op1Sel := OP1_RS1
      io.op2Sel := OP2_IMM
      io.imm    := immI
      io.aluOp  := ALU_ADD
      io.memLen := LEN_BYTE
    }
    .elsewhen(io.inst === SW) {
      io.memWen := true.B
      io.op1Sel := OP1_RS1
      io.op2Sel := OP2_IMM
      io.imm    := immS
      io.aluOp  := ALU_ADD
      io.memLen := LEN_WORD
    }
    .elsewhen(io.inst === SB) {
      io.memWen := true.B
      io.op1Sel := OP1_RS1
      io.op2Sel := OP2_IMM
      io.imm    := immS
      io.aluOp  := ALU_ADD
      io.memLen := LEN_BYTE
    }
    .elsewhen(io.inst === LUI) {
      io.imm    := immU
      io.regWen := true.B
      io.rdSel  := RD_IMM
    }
}
