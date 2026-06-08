package top
import chisel3._
import chisel3.util._

class AXI4IO extends Bundle {
  // AW (Write Address) Channel
  val awready = Input(Bool())
  val awvalid = Output(Bool())
  val awaddr  = Output(UInt(32.W))
  val awid    = Output(UInt(4.W))
  val awlen   = Output(UInt(8.W))
  val awsize  = Output(UInt(3.W))
  val awburst = Output(UInt(2.W))

  // W (Write Data) Channel
  val wready  = Input(Bool())
  val wvalid  = Output(Bool())
  val wdata   = Output(UInt(32.W))
  val wstrb   = Output(UInt(4.W))
  val wlast   = Output(Bool())

  // B (Write Response) Channel
  val bready  = Output(Bool())
  val bvalid  = Input(Bool())
  val bresp   = Input(UInt(2.W))
  val bid     = Input(UInt(4.W))

  // AR (Read Address) Channel
  val arready = Input(Bool())
  val arvalid = Output(Bool())
  val araddr  = Output(UInt(32.W))
  val arid    = Output(UInt(4.W))
  val arlen   = Output(UInt(8.W))
  val arsize  = Output(UInt(3.W))
  val arburst = Output(UInt(2.W))

  // R (Read Data) Channel
  val rready  = Output(Bool())
  val rvalid  = Input(Bool())
  val rresp   = Input(UInt(2.W))
  val rdata   = Input(UInt(32.W))
  val rlast   = Input(Bool())
  val rid     = Input(UInt(4.W))
}

