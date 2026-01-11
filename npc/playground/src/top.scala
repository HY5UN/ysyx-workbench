package lab

import chisel3._
import chisel3.util._

class top extends Module {
  val io = IO(new Bundle {
    val hex0 = Output(UInt(7.W))
    val hex1 = Output(UInt(7.W))
  })

  val PC   = RegInit(0.U(4.W))
  val Regs = RegInit(VecInit(Seq.fill(4)(0.U(8.W))))
  val ROM  = RegInit(
    VecInit(
      Seq(
        "h8A".U(8.W),
        "h90".U(8.W),
        "hA0".U(8.W),
        "hB1".U(8.W),
        "h17".U(8.W),
        "h29".U(8.W),
        "hD1".U(8.W),
        "h40".U(8.W)
      )
    )
  )


  val inst = ROM(PC)

  val inst_type = inst(7, 6)

  when(inst_type === "b00".U) {
    // add
    Regs(inst(5, 4)) := Regs(inst(3, 2)) + Regs(inst(1, 0))
    PC               := PC + 1.U
  }.elsewhen(inst_type === "b01".U) {
    // out rs
    PC := PC
  }.elsewhen(inst_type === "b10".U) {
    // li
    Regs(inst(5, 4)) := inst(3, 0)
    PC               := PC + 1.U
  }.elsewhen(inst_type === "b11".U) {
    // bner0
    when(Regs(inst(1, 0)) =/= Regs(0)) {
      PC := inst(5, 2)
    }.otherwise {
      PC := PC + 1.U
    }
  }.otherwise {
    PC := PC + 1.U
  }

  when(inst_type === "b01".U) {
    io.hex0 := SevenSeg.encodeHex0toF(Regs(2)(3, 0), true.B)
    io.hex1 := SevenSeg.encodeHex0toF(Regs(2)(7, 4), true.B)
  }.otherwise {
    io.hex0 := SevenSeg.encodeHex0toF(0.U, false.B)
    io.hex1 := SevenSeg.encodeHex0toF(0.U, false.B)
  }

}
