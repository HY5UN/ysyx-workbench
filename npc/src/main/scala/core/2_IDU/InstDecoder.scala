package top

import chisel3._
import chisel3.util._

import ControlConstants._
import RV32EInstr._


class RV32EDecoder extends Module {
  val io     = IO(new Bundle {
    val in = Flipped(Decoupled(new IFU2IDU))
    val out =Decoupled(new IDU2EXU)
    val rs1 = Output(UInt(32.W))
    val rs2 = Output(UInt(32.W))
  })
  val inst = io.in.bits.inst

  val rd     = inst(11, 7)
  val rs1    = inst(19, 15)
  val rs2    = inst(24, 20)

  val immI = inst(31, 20).asSInt.pad(32).asUInt
  val immS = Cat(inst(31, 25), inst(11, 7)).asSInt.pad(32).asUInt
  val immB = Cat(inst(31), inst(7), inst(30, 25), inst(11, 8), 0.U(1.W)).asSInt.pad(32).asUInt
  val immU = Cat(inst(31, 12), 0.U(12.W)).asSInt.pad(32).asUInt
  val immJ = Cat(inst(31), inst(19, 12), inst(20), inst(30, 21), 0.U(1.W)).asSInt.pad(32).asUInt

  val baseR       = Ctrl(op1Sel = OP1_RS1, op2Sel = OP2_RS2, rdSel = RD_ALU, regWen = Y)
  val baseI       = Ctrl(immSel = IMM_I, op1Sel = OP1_RS1, op2Sel = OP2_IMM, rdSel = RD_ALU, regWen = Y)
  val baseLoad    = Ctrl(immSel = IMM_I, op1Sel = OP1_RS1, op2Sel = OP2_IMM, rdSel = RD_MEM, regWen = Y, memR = Y, aluOp = ALU_ADD)
  val baseStore   = Ctrl(immSel = IMM_S, op1Sel = OP1_RS1, op2Sel = OP2_IMM, memWen = Y, aluOp = ALU_ADD)
  val baseBranch  = Ctrl(immSel = IMM_B, op1Sel = OP1_RS1, op2Sel = OP2_RS2, pcSel = PC_BRANCH)

  val decodeTable = Array(
    // R-type
    ADD  -> baseR.copy(aluOp = ALU_ADD).toList,
    SUB  -> baseR.copy(aluOp = ALU_SUB).toList,
    XOR  -> baseR.copy(aluOp = ALU_XOR).toList,
    OR   -> baseR.copy(aluOp = ALU_OR).toList,
    AND  -> baseR.copy(aluOp = ALU_AND).toList,
    SLL  -> baseR.copy(aluOp = ALU_LL).toList,
    SRL  -> baseR.copy(aluOp = ALU_RL).toList,
    SRA  -> baseR.copy(aluOp = ALU_RA).toList,
    SLT  -> baseR.copy(aluOp = ALU_LT).toList,
    SLTU -> baseR.copy(aluOp = ALU_LTU).toList,

    // I-type
    ADDI  -> baseI.copy(aluOp = ALU_ADD).toList,
    ANDI  -> baseI.copy(aluOp = ALU_AND).toList,
    ORI   -> baseI.copy(aluOp = ALU_OR).toList,
    XORI  -> baseI.copy(aluOp = ALU_XOR).toList,
    SLLI  -> baseI.copy(aluOp = ALU_LL).toList,
    SRLI  -> baseI.copy(aluOp = ALU_RL).toList,
    SRAI  -> baseI.copy(aluOp = ALU_RA).toList,
    SLTI  -> baseI.copy(aluOp = ALU_LT).toList,
    SLTIU -> baseI.copy(aluOp = ALU_LTU).toList,

    // Loads
    LB  -> baseLoad.copy(memLen = LEN_BYTE, memSext = Y).toList,
    LH  -> baseLoad.copy(memLen = LEN_HALF, memSext = Y).toList,
    LW  -> baseLoad.copy(memLen = LEN_WORD, memSext = N).toList,
    LBU -> baseLoad.copy(memLen = LEN_BYTE, memSext = N).toList,
    LHU -> baseLoad.copy(memLen = LEN_HALF, memSext = N).toList,

    // Stores
    SB -> baseStore.copy(memLen = LEN_BYTE).toList,
    SH -> baseStore.copy(memLen = LEN_HALF).toList,
    SW -> baseStore.copy(memLen = LEN_WORD).toList,

    // Branches
    BEQ  -> baseBranch.copy(aluOp = ALU_EQ).toList,
    BNE  -> baseBranch.copy(aluOp = ALU_NEQ).toList,
    BLT  -> baseBranch.copy(aluOp = ALU_LT).toList,
    BGE  -> baseBranch.copy(aluOp = ALU_GE).toList,
    BLTU -> baseBranch.copy(aluOp = ALU_LTU).toList,
    BGEU -> baseBranch.copy(aluOp = ALU_GEU).toList,

    // U-type
    LUI   -> Ctrl(immSel = IMM_U, rdSel = RD_IMM, regWen = Y).toList,
    AUIPC -> Ctrl(immSel = IMM_U, op1Sel = OP1_PC, op2Sel = OP2_IMM, rdSel = RD_ALU, regWen = Y, aluOp = ALU_ADD).toList,

    // Jumps
    JAL  -> Ctrl(immSel = IMM_J, regWen = Y, rdSel = RD_PC4, pcSel = PC_ALU, op1Sel = OP1_PC, op2Sel = OP2_IMM, aluOp = ALU_ADD).toList,
    JALR -> Ctrl(immSel = IMM_I, op1Sel = OP1_RS1, op2Sel = OP2_IMM, rdSel = RD_PC4, regWen = Y, aluOp = ALU_ADD, pcSel = PC_ALU1).toList,

    // CSR instructions
    CSRRW -> Ctrl(immSel = IMM_I, csrWen = Y, rdSel = RD_CSR, regWen = Y, csrSel = CSR_RS1).toList,
    CSRRS -> Ctrl(immSel = IMM_I, csrWen = Y, rdSel = RD_CSR, regWen = Y, csrSel = CSR_ALU, aluOp = ALU_OR, op2Sel = OP2_CSR).toList,

    // SYSTEM
    EBREAK -> Ctrl(ebreak = Y).toList,
    ECALL  -> Ctrl(ecall = Y, pcSel = PC_CSR, csrSel = CSR_PC ).toList,
    MRET   -> Ctrl(pcSel = PC_CSR, mret = Y).toList
  )
  
  

  val defaultCtrl = Ctrl().toList
  val ctrlSignals =  ListLookup(inst, defaultCtrl, decodeTable) 
  (io.out.bits.ctrl.getElements zip ctrlSignals.reverse).foreach {
    case (port: Bool, sig) => port := sig.asBool // 如果 Bundle 里是 Bool，自动转换
    case (port: UInt, sig) => port := sig        // 如果 Bundle 里是 UInt，直接连线
    case _ =>
  }

  io.out.bits.imm    := MuxLookup(io.out.bits.ctrl.immSel, 0.U)(
    Seq(
      IMM_I -> immI,
      IMM_S -> immS,
      IMM_B -> immB,
      IMM_U -> immU,
      IMM_J -> immJ
    )
  )
  when(!io.in.valid) {
    io.out.bits.ctrl.regWen  := false.B
    io.out.bits.ctrl.memWen  := false.B
    io.out.bits.ctrl.memR    := false.B
    io.out.bits.ctrl.csrWen  := false.B
    io.out.bits.ctrl.mret    := false.B
    io.out.bits.ctrl.ebreak  := false.B
    io.out.bits.ctrl.ecall   := false.B
  }
  
  io.rs1 := rs1
  io.rs2 := rs2
  io.out.bits.rd  := rd
  io.out.bits.pc  := io.in.bits.pc
  io.out.valid := io.in.valid
  io.in.ready := io.out.ready
}
