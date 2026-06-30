package top
import chisel3._
import chisel3.util._
class WBU2IFU extends Bundle {
  val fencei = Bool()
  val nextPC = UInt(32.W)
}
class WBU     extends Module {
  val io = IO(new Bundle {
    val in          = Flipped(Decoupled(new LSU2WBU))
    val out         = Decoupled(new WBU2IFU)
    val rd          = Output(UInt(5.W))
    val wen         = Output(Bool())
    val wdata       = Output(UInt(32.W))
    val csrWen      = Output(Bool())
    val csrWdata    = Output(UInt(32.W))
    val ecall       = Output(Bool())
    val mret        = Output(Bool())
    val wbuCsrRdata = Input(UInt(32.W))
    val branchTaken = Output(Bool())
  })

  val ctrl = io.in.bits.ctrl

  io.out.bits.fencei := ctrl.fencei

  when(io.in.valid) {
    io.wen    := ctrl.regWen
    io.ecall  := ctrl.ecall
    io.csrWen := ctrl.csrWen
    io.mret   := ctrl.mret
  }.otherwise {
    io.wen    := false.B
    io.ecall  := false.B
    io.csrWen := false.B
    io.mret   := false.B
  }

  io.rd    := io.in.bits.rd
  io.wdata := MuxLookup(ctrl.rdSel, io.in.bits.result)(
    Seq(
      RdSel.ALU -> io.in.bits.result,
      RdSel.MEM -> io.in.bits.memRdata,
      RdSel.PC4 -> (io.in.bits.pc + 4.U),
      RdSel.IMM -> io.in.bits.imm,
      RdSel.CSR -> io.in.bits.csrRdata
    )
  )

  io.out.bits.nextPC := MuxLookup(ctrl.pcSel, io.in.bits.pc + 4.U)(
    Seq(
      PcSel.NEXT   -> (io.in.bits.pc + 4.U),
      PcSel.ALU    -> (io.in.bits.result),
      PcSel.ALU1   -> (io.in.bits.result & "hfffffffe".U),
      PcSel.BRANCH -> Mux(io.in.bits.result(0), io.in.bits.pc + io.in.bits.imm, io.in.bits.pc + 4.U),
      PcSel.CSR    -> io.wbuCsrRdata
    )
  )

  io.csrWdata  := MuxLookup(ctrl.csrSel, 0.U)(
    Seq(
      CsrSel.RS1 -> io.in.bits.rdata1,
      CsrSel.ALU -> io.in.bits.result,
      CsrSel.PC  -> io.in.bits.pc
    )
  )
  io.in.ready  := true.B
  io.out.valid := false.B

  // 暂停流水 + 等待与ifu握手
  io.branchTaken := false.B
  when(ctrl.pcSel =/= PcSel.NEXT) {
    io.in.ready    := false.B
    io.out.valid   := true.B
    io.branchTaken := true.B
    when(io.out.ready) {
      io.in.ready := true.B
    }
  }

}
