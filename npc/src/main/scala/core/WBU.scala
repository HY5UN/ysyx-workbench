package top
import chisel3._
import chisel3.util._

class WriteBackUnit extends Module {
  val io = IO(new Bundle {
    val in       = Flipped(Decoupled(new LSU2WBU))
    val out      = Decoupled(new WBU2IFU)
    val rd       = Output(UInt(5.W))
    val wen      = Output(Bool())
    val wdata    = Output(UInt(32.W))
    val csrWen   = Output(Bool())
    val csrWdata = Output(UInt(32.W))
    val ecall    = Output(Bool())
    val mret     = Output(Bool())
    val csrRdata = Input(UInt(32.W))
  })

  val ctrl = io.in.bits.ctrl
  when(io.in.fire) {
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
      RdSel.CSR -> io.csrRdata
    )
  )

  io.out.bits.nextPC := MuxLookup(ctrl.pcSel, io.in.bits.pc + 4.U)(
    Seq(
      PcSel.NEXT      -> (io.in.bits.pc + 4.U),
      PcSel.ALU    -> (io.in.bits.result),
      PcSel.ALU1   -> (io.in.bits.result & "hfffffffe".U),
      PcSel.BRANCH -> Mux(io.in.bits.result(0), io.in.bits.pc + io.in.bits.imm, io.in.bits.pc + 4.U),
      PcSel.CSR    -> io.csrRdata
    )
  )

  io.csrWdata  := MuxLookup(ctrl.csrSel, 0.U)(
    Seq(
      CsrSel.RS1 -> io.in.bits.rdata1,
      CsrSel.ALU -> io.in.bits.result,
      CsrSel.PC  -> io.in.bits.pc
    )
  )
  io.in.ready  := io.out.ready
  io.out.valid := io.in.valid

}
