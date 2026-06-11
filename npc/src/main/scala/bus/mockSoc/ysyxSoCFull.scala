package top
import chisel3._
import chisel3.util._

class ysyxSoCFull extends Module {
  val io = IO(new Bundle {})

  val core = Module(new ysyx_26010036)

  val mem   = Module(new MemExt)
  val uart  = Module(new UART)
  val clint = Module(new CLINT)

  val xbar = Module(new MemXbar)
  core.io.master <> xbar.io.s
  xbar.io.mRAM <> mem.io.axi
  xbar.io.mUART <> uart.io.axi
  xbar.io.mCLINT <> clint.io.axi

}
