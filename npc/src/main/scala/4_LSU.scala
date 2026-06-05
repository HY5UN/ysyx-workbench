package top

import chisel3._
import chisel3.util._
import ControlConstants._

// ─────────────────────────────────────────────────────────────────────────────
// MemExt：内存黑盒，AXI4-Lite Slave 侧 → 用 Flipped(new AXI4LiteIO) 翻转方向
// ─────────────────────────────────────────────────────────────────────────────
class MemExt extends ExtModule {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val axi   = Flipped(new AXI4LiteIO) // slave 侧：araddr 等变为 Input，arready 等变为 Output
  })
}

// ─────────────────────────────────────────────────────────────────────────────
// LoadStoreUnit：AXI4-Lite Master，通过 mem.io.axi 访问 MemExt
// ─────────────────────────────────────────────────────────────────────────────
class LoadStoreUnit extends Module {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new EXU2LSU))
    val out = Decoupled(new LSU2WBU)
  })

  val ctrl = io.in.bits.ctrl

  object State extends ChiselEnum {
    val sIdle, sWait = Value
  }
  val state        = RegInit(State.sIdle)
  val memRdataReg  = RegInit(0.U(32.W))
  val memFinishReg = RegInit(false.B)

  // ── AXI 寄存器 ──────────────────────────────────────────────────────────────
  val araddrReg  = RegInit(0.U(32.W))
  val arvalidReg = RegInit(false.B)
  val rreadyReg  = RegInit(false.B)
  val awaddrReg  = RegInit(0.U(32.W))
  val awvalidReg = RegInit(false.B)
  val wvalidReg  = RegInit(false.B)
  val breadyReg  = RegInit(false.B)

  // ── MemExt 实例化，时钟/复位单独连接，其余通过 axi bundle ──────────────────
  val mem = Module(new MemExt())
  mem.io.clock := clock
  mem.io.reset := reset

  // —— 写地址 / 写数据（组合逻辑，直接来自寄存器和运算结果）——
  mem.io.axi.araddr  := araddrReg
  mem.io.axi.arvalid := arvalidReg
  mem.io.axi.rready  := rreadyReg
  mem.io.axi.awaddr  := awaddrReg
  mem.io.axi.awvalid := awvalidReg
  mem.io.axi.wvalid  := wvalidReg
  mem.io.axi.bready  := breadyReg

  // 写数据：按字节偏移移位
  mem.io.axi.wdata := io.in.bits.rdata2 << (io.in.bits.result(1, 0) * 8.U)
  // 写字节选通
  mem.io.axi.wstrb := MuxLookup(ctrl.memLen, "b0000".U)(
    Seq(
      LEN_BYTE -> ("b0001".U << io.in.bits.result(1, 0)),
      LEN_HALF -> Mux(io.in.bits.result(1), "b1100".U, "b0011".U),
      LEN_WORD -> "b1111".U
    )
  )

  // ── 读数据对齐 ──────────────────────────────────────────────────────────────
  val bytes = VecInit.tabulate(4)(i => mem.io.axi.rdata(8 * i + 7, 8 * i))
  val b      = bytes(io.in.bits.result(1, 0))
  val h      = Mux(io.in.bits.result(1), Cat(bytes(3), bytes(2)), Cat(bytes(1), bytes(0)))
  val readByte    = Mux(ctrl.memSext, Cat(Fill(24, b(7)), b), Cat(0.U(24.W), b))
  val readHalf    = Mux(ctrl.memSext, Cat(Fill(16, h(15)), h), Cat(0.U(16.W), h))
  val memReadData = MuxLookup(ctrl.memLen, mem.io.axi.rdata)(
    Seq(
      LEN_BYTE -> readByte,
      LEN_HALF -> readHalf,
      LEN_WORD -> mem.io.axi.rdata
    )
  )

  val isLS = ctrl.memR || ctrl.memWen
  memFinishReg := false.B // 默认每周期清零，由 sWait 完成握手时置高一拍

  // ── 随机延迟模块 ────────────────────────────────────────────────────────────
  val arvalidDelay = Module(new RandomDelay(3))
  val awvalidDelay = Module(new RandomDelay(4))
  val wvalidDelay  = Module(new RandomDelay(5))
  val rreadyDelay  = Module(new RandomDelay(4))
  val breadyDelay  = Module(new RandomDelay(3))
  arvalidDelay.io.trigger := false.B
  awvalidDelay.io.trigger := false.B
  wvalidDelay.io.trigger  := false.B
  rreadyDelay.io.trigger  := false.B
  breadyDelay.io.trigger  := false.B

  // ── FSM ────────────────────────────────────────────────────────────────────
  switch(state) {
    is(State.sIdle) {
      when(io.in.valid && isLS && !memFinishReg) {
        // 触发地址/数据通道延迟
        arvalidDelay.io.trigger := !ctrl.memWen
        awvalidDelay.io.trigger := ctrl.memWen
        wvalidDelay.io.trigger  := ctrl.memWen

        araddrReg := io.in.bits.result
        awaddrReg := io.in.bits.result

        when(ctrl.memWen) {
          // 写请求握手：awvalid & awready & wvalid & wready
          when(mem.io.axi.awready && mem.io.axi.wready && awvalidReg && wvalidReg) {
            breadyDelay.io.trigger := true.B
          }
        }.otherwise {
          // 读请求握手：arvalid & arready
          when(mem.io.axi.arready && arvalidReg) {
            rreadyDelay.io.trigger := true.B
          }
        }
      }

      // valid 信号逐步置位（延迟后 sticky）
      arvalidReg := arvalidReg || arvalidDelay.io.ready
      awvalidReg := awvalidReg || awvalidDelay.io.ready
      wvalidReg  := wvalidReg  || wvalidDelay.io.ready

      // bready 就绪后进入 sWait（写）
      when(!breadyReg) {
        when(breadyDelay.io.ready) {
          breadyReg := true.B
          state     := State.sWait
        }
      }
      // rready 就绪后进入 sWait（读）
      when(!rreadyReg) {
        when(rreadyDelay.io.ready) {
          rreadyReg := true.B
          state     := State.sWait
        }
      }
    }

    is(State.sWait) {
      // 地址/数据通道握手完成后撤销 valid
      arvalidReg := false.B
      awvalidReg := false.B
      wvalidReg  := false.B

      // 等待读/写响应握手
      when(mem.io.axi.rvalid || mem.io.axi.bvalid) {
        state        := State.sIdle
        memRdataReg  := memReadData
        rreadyReg    := false.B
        breadyReg    := false.B
        memFinishReg := true.B
      }
    }
  }

  // ── 输出 ───────────────────────────────────────────────────────────────────
  io.out.bits.ctrl     := ctrl
  io.out.bits.result   := io.in.bits.result
  io.out.bits.pc       := io.in.bits.pc
  io.out.bits.memRdata := memRdataReg
  io.out.bits.imm      := io.in.bits.imm
  io.out.bits.csrRdata := io.in.bits.csrRdata
  io.out.bits.rd       := io.in.bits.rd
  io.out.bits.rdata1   := io.in.bits.rdata1

  io.out.valid := io.in.valid && ((state === State.sIdle && !isLS) || memFinishReg)
  io.in.ready  := state === State.sIdle
}