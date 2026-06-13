package top

import chisel3._
import chisel3.util._

import ControlConstants._
import RV32EInstr._


class RV32EDecoder extends Module {
  val io     = IO(new Bundle {
    val in = Flipped(Decoupled(new IFU2IDU))
    val out = Decoupled(new IDU2EXU)
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

  val baseR      = Ctrl(op1Sel = Op1Sel.RS1, op2Sel = Op2Sel.RS2, rdSel = RdSel.ALU, regWen = true.B)
  val baseI      = Ctrl(immSel = ImmSel.I, op1Sel = Op1Sel.RS1, op2Sel = Op2Sel.IMM, rdSel = RdSel.ALU, regWen = true.B)
  val baseLoad   = Ctrl(immSel = ImmSel.I, op1Sel = Op1Sel.RS1, op2Sel = Op2Sel.IMM, rdSel = RdSel.MEM, regWen = true.B, memR = true.B, aluOp = AluOp.ADD)
  val baseStore  = Ctrl(immSel = ImmSel.S, op1Sel = Op1Sel.RS1, op2Sel = Op2Sel.IMM, memWen = true.B, aluOp = AluOp.ADD)
  val baseBranch = Ctrl(immSel = ImmSel.B, op1Sel = Op1Sel.RS1, op2Sel = Op2Sel.RS2, pcSel = PcSel.BRANCH)

  val decodeTable = Array(
    // R-type
    ADD  -> baseR.copy(aluOp = AluOp.ADD).toList,
    SUB  -> baseR.copy(aluOp = AluOp.SUB).toList,
    XOR  -> baseR.copy(aluOp = AluOp.XOR).toList,
    OR   -> baseR.copy(aluOp = AluOp.OR).toList,
    AND  -> baseR.copy(aluOp = AluOp.AND).toList,
    SLL  -> baseR.copy(aluOp = AluOp.LL).toList,
    SRL  -> baseR.copy(aluOp = AluOp.RL).toList,
    SRA  -> baseR.copy(aluOp = AluOp.RA).toList,
    SLT  -> baseR.copy(aluOp = AluOp.LT).toList,
    SLTU -> baseR.copy(aluOp = AluOp.LTU).toList,

    // I-type
    ADDI  -> baseI.copy(aluOp = AluOp.ADD).toList,
    ANDI  -> baseI.copy(aluOp = AluOp.AND).toList,
    ORI   -> baseI.copy(aluOp = AluOp.OR).toList,
    XORI  -> baseI.copy(aluOp = AluOp.XOR).toList,
    SLLI  -> baseI.copy(aluOp = AluOp.LL).toList,
    SRLI  -> baseI.copy(aluOp = AluOp.RL).toList,
    SRAI  -> baseI.copy(aluOp = AluOp.RA).toList,
    SLTI  -> baseI.copy(aluOp = AluOp.LT).toList,
    SLTIU -> baseI.copy(aluOp = AluOp.LTU).toList,

    // Loads
    LB  -> baseLoad.copy(memLen = MemLen.BYTE, memSext = true.B).toList,
    LH  -> baseLoad.copy(memLen = MemLen.HALF, memSext = true.B).toList,
    LW  -> baseLoad.copy(memLen = MemLen.WORD).toList,
    LBU -> baseLoad.copy(memLen = MemLen.BYTE).toList,
    LHU -> baseLoad.copy(memLen = MemLen.HALF).toList,

    // Stores
    SB -> baseStore.copy(memLen = MemLen.BYTE).toList,
    SH -> baseStore.copy(memLen = MemLen.HALF).toList,
    SW -> baseStore.copy(memLen = MemLen.WORD).toList,

    // Branches
    BEQ  -> baseBranch.copy(aluOp = AluOp.EQ).toList,
    BNE  -> baseBranch.copy(aluOp = AluOp.NEQ).toList,
    BLT  -> baseBranch.copy(aluOp = AluOp.LT).toList,
    BGE  -> baseBranch.copy(aluOp = AluOp.GE).toList,
    BLTU -> baseBranch.copy(aluOp = AluOp.LTU).toList,
    BGEU -> baseBranch.copy(aluOp = AluOp.GEU).toList,

    // U-type
    LUI   -> Ctrl(immSel = ImmSel.U, rdSel = RdSel.IMM, regWen = true.B).toList,
    AUIPC -> Ctrl(immSel = ImmSel.U, op1Sel = Op1Sel.PC, op2Sel = Op2Sel.IMM, rdSel = RdSel.ALU, regWen = true.B, aluOp = AluOp.ADD).toList,

    // Jumps
    JAL  -> Ctrl(immSel = ImmSel.J, regWen = true.B, rdSel = RdSel.PC4, pcSel = PcSel.ALU,  op1Sel = Op1Sel.PC,  op2Sel = Op2Sel.IMM, aluOp = AluOp.ADD).toList,
    JALR -> Ctrl(immSel = ImmSel.I, op1Sel = Op1Sel.RS1, op2Sel = Op2Sel.IMM, rdSel = RdSel.PC4, regWen = true.B, aluOp = AluOp.ADD, pcSel = PcSel.ALU1).toList,

    // CSR
    CSRRW -> Ctrl(immSel = ImmSel.I, csrWen = true.B, rdSel = RdSel.CSR, regWen = true.B, csrSel = CsrSel.RS1).toList,
    CSRRS -> Ctrl(immSel = ImmSel.I, csrWen = true.B, rdSel = RdSel.CSR, regWen = true.B, csrSel = CsrSel.ALU, aluOp = AluOp.OR, op2Sel = Op2Sel.CSR).toList,

    // SYSTEM
    EBREAK -> Ctrl(ebreak = true.B).toList,
    ECALL  -> Ctrl(ecall = true.B, pcSel = PcSel.CSR, csrSel = CsrSel.PC).toList,
    MRET   -> Ctrl(pcSel = PcSel.CSR, mret = true.B).toList
  )
  
  

  val defaultCtrl = Ctrl().toList
  val ctrlSignals =  ListLookup(inst, defaultCtrl, decodeTable) 
  (io.out.bits.ctrl.getElements zip ctrlSignals.reverse).foreach {
  case (port, sig) => port := sig.asTypeOf(port)
}

  io.out.bits.imm    := MuxLookup(io.out.bits.ctrl.immSel, 0.U)(
    Seq(
      ImmSel.I -> immI,
      ImmSel.S -> immS,
      ImmSel.B -> immB,
      ImmSel.U -> immU,
      ImmSel.J -> immJ
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
