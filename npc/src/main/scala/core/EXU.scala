package top

import chisel3._
import chisel3.util._

class EXU2LSU extends Bundle {
  val result   = UInt(32.W)
  val ctrl     = new CtrlBundle
  val rdata1   = UInt(32.W)
  val rdata2   = UInt(32.W)
  val pc       = UInt(32.W)
  val imm      = UInt(32.W)
  val rd       = UInt(5.W)
  val csrRdata = UInt(32.W)
  val npc      = UInt(32.W)
  val inst     = UInt(32.W)
}
class EXU     extends Module {
  val io   = IO(new Bundle {
    val in         = Flipped(Decoupled(new IDU2EXU))
    val out        = Decoupled(new EXU2LSU)
    val flush      = Input(Bool())
    val redirectEn = Output(Bool())
    val redirectPc = Output(UInt(32.W))
  })
  val ctrl = io.in.bits.ctrl

  val alu = Module(new ALU())

  alu.io.op1  := Mux(ctrl.op1Sel === Op1Sel.RS1, io.in.bits.rdata1, io.in.bits.pc)
  alu.io.op2  := MuxLookup(ctrl.op2Sel, io.in.bits.rdata2)(
    Seq(
      Op2Sel.RS2 -> io.in.bits.rdata2,
      Op2Sel.IMM -> io.in.bits.imm,
      Op2Sel.CSR -> io.in.bits.csrRdata
    )
  )
  alu.io.ctrl := ctrl

  io.out.bits.result   := alu.io.result
  io.out.bits.ctrl     := ctrl
  io.out.bits.rdata1   := io.in.bits.rdata1
  io.out.bits.rdata2   := io.in.bits.rdata2
  io.out.bits.csrRdata := io.in.bits.csrRdata
  io.out.bits.pc       := io.in.bits.pc
  io.out.bits.imm      := io.in.bits.imm
  io.out.bits.rd       := io.in.bits.rd

  io.out.valid := io.in.valid && !io.flush
  io.in.ready  := io.out.ready

  when(!io.in.valid) {
    io.out.bits.ctrl.regWen   := false.B
    io.out.bits.ctrl.memWen   := false.B
    io.out.bits.ctrl.memR     := false.B
    io.out.bits.ctrl.csrWen   := false.B
    io.out.bits.ctrl.mret     := false.B
    io.out.bits.ctrl.excValid := false.B
  }

  val nextPc = MuxLookup(ctrl.pcSel, alu.io.result)(
    Seq(
      PcSel.ALU    -> alu.io.result,
      PcSel.ALU1   -> (alu.io.result & "hfffffffe".U),
      // PcSel.BRANCH -> Mux(alu.io.cmpResult, io.in.bits.pc + io.in.bits.imm, io.in.bits.pc + 4.U)
      PcSel.BRANCH -> io.in.bits.pc + io.in.bits.imm
    )
  )
  io.redirectPc := nextPc
  io.redirectEn:= false.B
  when(ctrl.pcSel =/= PcSel.NEXT && !ctrl.excValid && io.in.valid ){
    io.redirectEn := true.B
    when(ctrl.pcSel === PcSel.BRANCH && !alu.io.cmpResult){
      io.redirectEn := false.B
    }
  }
  io.out.bits.npc  := nextPc
  io.out.bits.inst := io.in.bits.inst

  when(ctrl.excValid) {
    io.out.bits.ctrl.excType  := ctrl.excType
    io.out.bits.ctrl.excValid := true.B
  }
}

class ALU extends Module {
  val io = IO(new Bundle {
    val op1       = Input(UInt(32.W))
    val op2       = Input(UInt(32.W))
    val ctrl      = Input(new CtrlBundle)
    val result    = Output(UInt(32.W))
    val cmpResult = Output(Bool())
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

  }
  switch(io.ctrl.aluOp) {
    is(AluOp.LT) { io.cmpResult := (io.op1.asSInt < io.op2.asSInt).asUInt }
    is(AluOp.LTU) { io.cmpResult := (io.op1 < io.op2).asUInt }
    is(AluOp.EQ) { io.cmpResult := (io.op1 === io.op2).asUInt }
    is(AluOp.NEQ) { io.cmpResult := (io.op1 =/= io.op2).asUInt }
    is(AluOp.GE) { io.cmpResult := (io.op1.asSInt >= io.op2.asSInt).asUInt }
    is(AluOp.GEU) { io.cmpResult := (io.op1 >= io.op2).asUInt }
  }

}
