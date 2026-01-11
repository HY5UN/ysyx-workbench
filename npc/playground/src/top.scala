package lab

import chisel3._
import chisel3.util._

class top extends Module {
  val io = IO(new Bundle {
    val hex0 = Output( UInt(7.W))
  })

  val PC = RegInit(0.U(4.W))
  val Regs = RegInit(VecInit(Seq.fill(4)(0.U(4.W))))
  val rom = Module(new sCPUROM)
  rom.PC := PC
  val inst = rom.inst

  val inst_type = inst(7, 6)

  when (inst_type === "b00".U) {
    // add
    Regs(inst(5, 4)) := Regs(inst(3, 2)) + Regs(inst(1, 0))
    PC := PC + 1.U
  }.elsewhen(inst_type === "b01".U) {
    // out rs
    PC:=PC 
  }
  .elsewhen (inst_type === "b10".U) {
    // li
    Regs(inst(5, 4)) := inst(3, 0)
    PC := PC + 1.U
  } .elsewhen (inst_type === "b11".U) {
    // bner0 
    when (Regs(inst(1,0)) =/= Regs(0)) {
      PC := inst(5, 2)
    } .otherwise {
      PC := PC + 1.U
    }
  } .otherwise {
    PC := PC + 1.U
  }

  when(inst_type === "b01".U) {
    io.hex0 := SevenSeg.encodeHex0toF(Regs(2), true.B)
  } .otherwise {
    io.hex0 := SevenSeg.encodeHex0toF(0.U, false.B)
  }


}