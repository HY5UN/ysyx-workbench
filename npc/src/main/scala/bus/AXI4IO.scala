package top
import chisel3._
import chisel3.util._

//master视角
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

// 正向 Master 置零模块：所有输出（awvalid, wvalid, bready, arvalid, rready 等）输出 0
class AXI4MasterTie0 extends Module {
  val io = IO(new Bundle {
    val m = new AXI4IO   // 正向 master 端口
  })

  // 将所有 Output 方向的信号驱动为 0
  io.m.awvalid := false.B
  io.m.awaddr  := 0.U
  io.m.awid    := 0.U
  io.m.awlen   := 0.U
  io.m.awsize  := 0.U
  io.m.awburst := 0.U

  io.m.wvalid  := false.B
  io.m.wdata   := 0.U
  io.m.wstrb   := 0.U
  io.m.wlast   := false.B

  io.m.bready  := false.B

  io.m.arvalid := false.B
  io.m.araddr  := 0.U
  io.m.arid    := 0.U
  io.m.arlen   := 0.U
  io.m.arsize  := 0.U
  io.m.arburst := 0.U

  io.m.rready  := false.B
}

// 反向 Slave 置零模块：Flipped 后，原来 AXI4IO 中的 Input 变成了 Output
// 需要将这些输出驱动为 0（例如 awready, wready, bvalid, arready, rvalid 等）
class AXI4SlaveTie0 extends Module {
  val io = IO(new Bundle {
    val s = Flipped(new AXI4IO)   // 反向 slave 端口
  })

  // 驱动所有经过 Flipped 后变为 Output 的信号为 0
  io.s.awready := false.B
  io.s.wready  := false.B
  io.s.bvalid  := false.B
  io.s.bresp   := 0.U
  io.s.bid     := 0.U

  io.s.arready := false.B
  io.s.rvalid  := false.B
  io.s.rresp   := 0.U
  io.s.rdata   := 0.U
  io.s.rlast   := false.B
  io.s.rid     := 0.U
}

class AXI4Data extends Bundle {
  // AW Channel
  val awvalid = Bool()
  val awaddr  = UInt(32.W)
  val awid    = UInt(4.W)
  val awlen   = UInt(8.W)
  val awsize  = UInt(3.W)
  val awburst = UInt(2.W)

  // W Channel
  val wvalid  = Bool()
  val wdata   = UInt(32.W)
  val wstrb   = UInt(4.W)
  val wlast   = Bool()

  // B Channel
  val bready  = Bool()

  // AR Channel
  val arvalid = Bool()
  val araddr  = UInt(32.W)
  val arid    = UInt(4.W)
  val arlen   = UInt(8.W)
  val arsize  = UInt(3.W)
  val arburst = UInt(2.W)

  // R Channel
  val rready  = Bool()
}