package top
import chisel3._
import chisel3.util._


class IFU2IDU extends Bundle {
  val inst = UInt(32.W)
  val pc   = UInt(32.W)
}

class IDU2EXU extends Bundle {
  val rd  = UInt(5.W)
  val imm = UInt(32.W)
  val pc  = UInt(32.W)

  val ctrl = new CtrlBundle
}

class EXU2LSU extends Bundle {
  val result   = UInt(32.W)
  val ctrl     = new CtrlBundle
  val rdata1   = UInt(32.W)
  val rdata2   = UInt(32.W)
  val pc       = UInt(32.W)
  val imm      = UInt(32.W)
  val csrRdata = UInt(32.W)
  val rd       = UInt(5.W)
}

class LSU2WBU extends Bundle {
  val memRdata = UInt(32.W)
  val ctrl     = new CtrlBundle
  val result   = UInt(32.W)
  val pc       = UInt(32.W)
  val imm      = UInt(32.W)
  val csrRdata = UInt(32.W)
  val rd       = UInt(5.W)
  val rdata1   = UInt(32.W)
}

class WBU2IFU extends Bundle {
  
  val nextPC = UInt(32.W)
}