package top
import chisel3._
import chisel3.util._

class WBU     extends Module {
  val io = IO(new Bundle {
    val in          = Flipped(Decoupled(new LSU2WBU))
    val rd          = Output(UInt(5.W))
    val wen         = Output(Bool())
    val wdata       = Output(UInt(32.W))
    val csrWen      = Output(Bool())
    val csrWdata    = Output(UInt(32.W))
    val nextPc = Output(UInt(32.W))

    val wbuCsrRdata = Input(UInt(32.W))
    val excType= Output(ExceptionType())
    val excValid = Output(Bool())
    val mret = Output(Bool())

    val redirectEn = Output(Bool())
    val redirectPc =Output(UInt(32.W))
  })

  val ctrl = io.in.bits.ctrl


  when(io.in.valid) {
    io.wen    := ctrl.regWen
    io.csrWen := ctrl.csrWen
    io.mret   := ctrl.mret
    io.excValid := ctrl.excValid
  }.otherwise {
    io.wen    := false.B
    io.csrWen := false.B
    io.mret   := false.B
    io.excValid := false.B
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

  //仅用于dpic
  io.nextPc := Mux(io.redirectEn, io.redirectPc, io.in.bits.npc)

  io.redirectPc := io.wbuCsrRdata

  io.csrWdata  := MuxLookup(ctrl.csrSel, 0.U)(
    Seq(
      CsrSel.RS1 -> io.in.bits.rdata1,
      CsrSel.ALU -> io.in.bits.result,
      CsrSel.PC  -> io.in.bits.pc
    )
  )
  io.excType := ctrl.excType

  io.in.ready  := true.B

  io.redirectEn := (ctrl.mret|| ctrl.excValid) && io.in.valid

}
