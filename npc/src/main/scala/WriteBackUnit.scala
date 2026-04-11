package top
import chisel3._
import chisel3.util._
import ControlConstants._

class WriteBackUnit extends Module {
  val io = IO(new Bundle {
    val in       = Flipped(Decoupled(new LSU2WBU))
    val rd       = Output(UInt(5.W))
    val wen      = Output(Bool())
    val wdata    = Output(UInt(32.W))
    val nextPC   = Output(UInt(32.W))
    val csrWen   = Output(Bool())
    val csrWdata = Output(UInt(32.W))
  })

  val ctrl = io.in.bits.ctrl

  io.rd    := io.in.bits.rd
  io.wen   := ctrl.regWen
  io.wdata := MuxLookup(ctrl.rdSel, io.in.bits.result)(
    Seq(
      RD_ALU -> io.in.bits.result,
      RD_MEM -> io.in.bits.memRdata,
      RD_PC4 -> (io.in.bits.pc + 4.U),
      RD_IMM -> io.in.bits.imm,
      RD_CSR -> io.in.bits.csrRdata
    )
  )

  io.nextPC := MuxLookup(ctrl.pcSel, io.in.bits.pc + 4.U)(
    Seq(
      PC_4      -> (io.in.bits.pc + 4.U),
      PC_ALU    -> (io.in.bits.result),
      PC_ALU1   -> (io.in.bits.result & "hfffffffe".U),
      PC_BRANCH -> Mux(io.in.bits.result(0), io.in.bits.pc + io.in.bits.imm, io.in.bits.pc + 4.U),
      PC_CSR    -> io.in.bits.csrRdata
    )
  )

  io.csrWen   := ctrl.csrWen
  io.csrWdata := MuxLookup(ctrl.csrSel, 0.U)(
    Seq(
      CSR_RS1 -> io.in.bits.rdata1,
      CSR_ALU -> io.in.bits.result,
      CSR_PC  -> io.in.bits.pc
    )
  )

}
