package top

import chisel3._
import chisel3.util._
class LSU2WBU extends Bundle {
  val memRdata = UInt(32.W)
  val ctrl     = new CtrlBundle
  val result   = UInt(32.W)
  val pc       = UInt(32.W)
  val imm      = UInt(32.W)
  val rd       = UInt(5.W)
  val rdata1   = UInt(32.W)
  val csrRdata = UInt(32.W)
}
class LSU     extends Module {
  val io     = IO(new Bundle {
    val in  = Flipped(Decoupled(new EXU2LSU))
    val out = Decoupled(new LSU2WBU)
    val axi = new AXI4IO
  })

  ChiselUtils.driveZeroOutputs(io.axi)
  
  io.axi.wlast := true.B

  val inReg = RegEnable(io.in.bits, io.in.fire)

  // 组合逻辑解码
  val ctrl  = inReg.ctrl
  val wdata = inReg.rdata2 << (inReg.result(1, 0) * 8.U)
  val wstrb = MuxLookup(ctrl.memLen, "b0000".U)(
    Seq(
      MemLen.BYTE -> ("b0001".U << inReg.result(1, 0)),
      MemLen.HALF -> Mux(inReg.result(1), "b1100".U, "b0011".U),
      MemLen.WORD -> "b1111".U
    )
  )

  val bytes       = VecInit.tabulate(4)(i => io.axi.rdata(8 * i + 7, 8 * i))
  val b           = bytes(inReg.result(1, 0))
  val h           = Mux(inReg.result(1), Cat(bytes(3), bytes(2)), Cat(bytes(1), bytes(0)))
  val readByte    = Mux(ctrl.memSext, Cat(Fill(24, b(7)), b), Cat(0.U(24.W), b))
  val readHalf    = Mux(ctrl.memSext, Cat(Fill(16, h(15)), h), Cat(0.U(16.W), h))
  val memReadData = MuxLookup(ctrl.memLen, io.axi.rdata)(
    Seq(
      MemLen.BYTE -> readByte,
      MemLen.HALF -> readHalf,
      MemLen.WORD -> io.axi.rdata
    )
  )

  // 状态机控制AXI4读写事务
  val memRdataReg = RegInit(0.U(32.W))
  val memAddr     = inReg.result
  object State extends ChiselEnum {
    val sIdle, sArWait, sAwWait, sRWait, sBWait, sOut = Value
  }
  val state = RegInit(State.sIdle)
  switch(state) {
    is(State.sIdle) {
      when(io.in.fire) {
        when(io.in.bits.ctrl.memR) {
          state := State.sArWait
        }.elsewhen(io.in.bits.ctrl.memWen) {
          state := State.sAwWait
        }.otherwise {
          state := State.sOut
        }
      }
    }
    is(State.sArWait) {
      when(io.axi.arvalid && io.axi.arready) {
        state := State.sRWait
      }
    }
    is(State.sAwWait) {
      when(io.axi.awvalid && io.axi.awready) {
        state := State.sBWait
      }
    }
    is(State.sRWait) {
      when(io.axi.rvalid && io.axi.rready) {
        state       := State.sOut
        memRdataReg := memReadData
      }
    }
    is(State.sBWait) {
      when(io.axi.bvalid && io.axi.bready) {
        state := State.sOut
      }
    }
    is(State.sOut) {
      when(io.out.fire) {
        state := State.sIdle
      }
    }
  }
  io.axi.araddr := memAddr
  io.axi.arvalid := state === State.sArWait
  io.axi.arsize  := ctrl.memLen
  io.axi.rready  := state === State.sRWait
  io.axi.arlen  := 0.U


  io.axi.awaddr  := memAddr
  io.axi.awvalid := state === State.sAwWait
  io.axi.awlen   := 0.U
  io.axi.wdata   := wdata
  io.axi.wstrb   := wstrb
  io.axi.wvalid  := state === State.sAwWait
  io.axi.awsize  := ctrl.memLen
  io.axi.bready  := state === State.sBWait

  inReg.elements.foreach { case (name, data) =>
    if (io.out.bits.elements.contains(name))
      io.out.bits.elements(name) := data
  }
  io.out.bits.memRdata := memRdataReg

  io.out.valid := state === State.sOut
  io.in.ready  := state === State.sIdle
}
