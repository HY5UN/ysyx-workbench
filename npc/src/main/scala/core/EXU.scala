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
  val pfm_tag  = UInt(8.W)
}
class EXU     extends Module {
  val io   = IO(new Bundle {
    val in         = Flipped(Decoupled(new IDU2EXU))
    val out        = Decoupled(new EXU2LSU)
    val redirectEn = Output(Bool())
    val redirectPc = Output(UInt(32.W))

  })
  val ctrl = io.in.bits.ctrl

  val alu = Module(new ALU())

  alu.io.op1  := io.in.bits.op1
  alu.io.op2  := io.in.bits.op2
    
  alu.io.ctrl := ctrl

  io.out.bits.result   := alu.io.result
  io.out.bits.ctrl     := ctrl
  io.out.bits.rdata1   := io.in.bits.rdata1
  io.out.bits.rdata2   := io.in.bits.rdata2
  io.out.bits.csrRdata := io.in.bits.csrRdata
  io.out.bits.pc       := io.in.bits.pc
  io.out.bits.imm      := io.in.bits.imm
  io.out.bits.rd       := io.in.bits.rd
  io.out.bits.pfm_tag  := io.in.bits.pfm_tag
  io.out.bits.inst := io.in.bits.inst

  io.out.valid := io.in.valid 
  io.in.ready  := io.out.ready 
  
  val nextPc = MuxLookup(ctrl.pcSel, io.in.bits.pc4)(
    Seq(
      PcSel.NEXT   -> io.in.bits.pc4,
      PcSel.ALU    -> alu.io.result,
      PcSel.ALU1   -> (alu.io.result & "hfffffffe".U),
      PcSel.BRANCH -> Mux(io.in.bits.branchTaken, alu.io.result, io.in.bits.pc4)
    )
  )
  io.redirectPc := nextPc
  io.redirectEn    := ctrl.pcSel =/= PcSel.NEXT && !ctrl.excValid && io.in.valid
  io.out.bits.npc  := nextPc

  // io.redirectEn:= false.B
  // io.redirectPc := 0.U
  // io.out.bits.npc := 0.U


  when(ctrl.excValid) {
    io.out.bits.ctrl.excType  := ctrl.excType
    io.out.bits.ctrl.excValid := true.B
  }
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
    is(AluOp.EQ) { io.result := (io.op1 === io.op2).asUInt }
    is(AluOp.NEQ) { io.result := (io.op1 =/= io.op2).asUInt }
    is(AluOp.GE) { io.result := (io.op1.asSInt >= io.op2.asSInt).asUInt }
    is(AluOp.GEU) { io.result := (io.op1 >= io.op2).asUInt }
  }

}
