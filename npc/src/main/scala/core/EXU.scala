package top

import chisel3._
import chisel3.util._

class EXU2LSU extends IDU2EXU {
  val result   = UInt(32.W)
  val gprWdata = UInt(32.W)
  val csrWdata = UInt(32.W)
  val dpic_npc = UInt(32.W)
}

class EXU extends Module {
  val io   = IO(new Bundle {
    val in  = Flipped(Decoupled(new IDU2EXU))
    val out = Decoupled(new EXU2LSU)

    val redirectEn = Output(Bool())
    val redirectPc = Output(UInt(32.W))

    val branch = Output(new BranchInfo) // unused

    val fenceiValid = Output(Bool())

    val dpic_branchCorrect = Output(Bool())
  })
  val ctrl = io.in.bits.ctrl

  val alu = Module(new ALU())
  alu.io.op1  := io.in.bits.op1
  alu.io.op2  := io.in.bits.op2
  alu.io.ctrl := ctrl

  BundleConnect(io.in.bits, io.out.bits)
  io.out.bits.result   := alu.io.result
  io.out.bits.gprWdata := MuxLookup(ctrl.rdSel, alu.io.result)(
    Seq(
      RdSel.ALU -> alu.io.result,
      RdSel.PC4 -> io.in.bits.pc4,
      RdSel.IMM -> io.in.bits.imm,
      RdSel.CSR -> io.in.bits.csrRdata
    )
  )
   io.out.bits.csrWdata := MuxLookup(ctrl.csrSel, io.in.bits.rdata1)(
    Seq(
      CsrSel.RS1 -> io.in.bits.rdata1,
      CsrSel.ALU -> alu.io.result
    )
  )

  io.out.valid := io.in.valid
  io.in.ready  := true.B

  val branchTaken = MuxLookup(ctrl.brOp, false.B)(
    Seq(
      BranchOp.EQ  -> (io.in.bits.rdata1 === io.in.bits.rdata2),
      BranchOp.NEQ -> (io.in.bits.rdata1 =/= io.in.bits.rdata2),
      BranchOp.LT  -> (io.in.bits.rdata1.asSInt < io.in.bits.rdata2.asSInt),
      BranchOp.GE  -> (io.in.bits.rdata1.asSInt >= io.in.bits.rdata2.asSInt),
      BranchOp.LTU -> (io.in.bits.rdata1 < io.in.bits.rdata2),
      BranchOp.GEU -> (io.in.bits.rdata1 >= io.in.bits.rdata2)
    )
  )
  val pcImm       = WireInit((io.in.bits.pc + io.in.bits.imm)(31, 0))
  val pcRs1       = WireInit(((io.in.bits.rdata1 + io.in.bits.imm) & "hfffffffe".U)(31, 0))
  io.redirectPc := MuxLookup(ctrl.pcSel, io.in.bits.pc4)(
    Seq(
      PcSel.IMM    -> pcImm,
      PcSel.RS1    -> pcRs1,
      PcSel.BRANCH -> Mux(branchTaken, pcImm, io.in.bits.pc4)
    )
  )
  // val branchCorrect = (ctrl.pcSel === PcSel.BRANCH && io.in.bits.branchPreTaken === branchTaken)
  val branchCorrect = (ctrl.pcSel === PcSel.BRANCH && !branchTaken)
  val needRedirect = !(ctrl.pcSel === PcSel.NEXT || branchCorrect)
  io.redirectEn := !ctrl.excValid && io.in.valid && (needRedirect || ctrl.fencei)

  io.fenceiValid := ctrl.fencei && !ctrl.excValid && io.in.valid

  io.branch.pc     := io.in.bits.pc
  io.branch.target := pcImm
  io.branch.dir    := io.in.bits.imm(12)
  io.branch.valid  := (ctrl.pcSel === PcSel.BRANCH) && !ctrl.excValid && io.in.valid
  io.branch.taken  := branchTaken

  io.out.bits.dpic_npc  := io.redirectPc
  io.dpic_branchCorrect := branchCorrect

}

class ALU extends Module {
  val io = IO(new Bundle {
    val op1    = Input(UInt(32.W))
    val op2    = Input(UInt(32.W))
    val ctrl   = Input(new CtrlBundle)
    val result = Output(UInt(32.W))
  })

  io.result := 0.U
  switch(io.ctrl.aluOp) {
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
  }

}
