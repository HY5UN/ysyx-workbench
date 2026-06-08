package top

import chisel3._
import chisel3.util._

class AXI4LiteIO extends Bundle {
  // ---------- Read Address Channel ----------
  val araddr  = Output(UInt(32.W))
  val arvalid = Output(Bool())
  val arready = Input(Bool())

  // ---------- Read Data Channel -------------
  val rdata  = Input(UInt(32.W))
  val rresp  = Input(UInt(2.W))
  val rvalid = Input(Bool())
  val rready = Output(Bool())

  // ---------- Write Address Channel ---------
  val awaddr  = Output(UInt(32.W))
  val awvalid = Output(Bool())
  val awready = Input(Bool())

  // ---------- Write Data Channel ------------
  val wdata  = Output(UInt(32.W))
  val wstrb  = Output(UInt(4.W))
  val wvalid = Output(Bool())
  val wready = Input(Bool())

  // ---------- Write Response Channel --------
  val bresp  = Input(UInt(2.W))
  val bvalid = Input(Bool())
  val bready = Output(Bool())
}

class AXI4LiteTie0 extends Module {
  val io = IO(new Bundle {
    val m = new AXI4LiteIO          // 正向：充当 master，对外输出 araddr/arvalid 等
    val s = Flipped(new AXI4LiteIO) // 反向：充当 slave，对外输出 arready/rdata 等
  })

  // ── 正向 master 侧：驱动所有 Output 为 0 ──────────────────────
  io.m.araddr  := 0.U
  io.m.arvalid := false.B
  io.m.rready  := false.B
  io.m.awaddr  := 0.U
  io.m.awvalid := false.B
  io.m.wdata   := 0.U
  io.m.wstrb   := 0.U
  io.m.wvalid  := false.B
  io.m.bready  := false.B

  // ── 反向 slave 侧：驱动所有 Output（在 Flipped 后变为 Input）为 0 ──
  io.s.arready := false.B
  io.s.rdata   := 0.U
  io.s.rresp   := 0.U
  io.s.rvalid  := false.B
  io.s.awready := false.B
  io.s.wready  := false.B
  io.s.bresp   := 0.U
  io.s.bvalid  := false.B
}
// 正向 Master：所有 Output 方向信号输出 0
class AXI4LiteMasterTie0 extends Module {
  val io = IO(new Bundle {
    val m = new AXI4LiteIO
  })

  // 写地址通道
  io.m.awvalid := false.B
  io.m.awaddr  := 0.U
  // 写数据通道
  io.m.wvalid  := false.B
  io.m.wdata   := 0.U
  io.m.wstrb   := 0.U
  // 写响应通道
  io.m.bready  := false.B
  // 读地址通道
  io.m.arvalid := false.B
  io.m.araddr  := 0.U
  // 读数据通道
  io.m.rready  := false.B
}
// 反向 Slave：Flipped 后，原 Bundle 中的 Input 变为 Output，需驱动为 0
class AXI4LiteSlaveTie0 extends Module {
  val io = IO(new Bundle {
    val s = Flipped(new AXI4LiteIO)
  })

  // 写地址通道
  io.s.awready := false.B
  // 写数据通道
  io.s.wready  := false.B
  // 写响应通道
  io.s.bvalid  := false.B
  io.s.bresp   := 0.U
  // 读地址通道
  io.s.arready := false.B
  // 读数据通道
  io.s.rvalid  := false.B
  io.s.rdata   := 0.U
  io.s.rresp   := 0.U
}

