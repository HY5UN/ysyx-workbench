package top

import chisel3._
import chisel3.util._

class LoadStoreUnit extends Module {
  val io     = IO(new Bundle {
    val in  = Flipped(Decoupled(new EXU2LSU))
    val out = Decoupled(new LSU2WBU)
    val axi = new AXI4IO
  })
  val axiReg = RegInit(0.U.asTypeOf(new AXI4Out))
  // axiReg.elements.foreach { case (name, data) =>
  //   io.axi.elements(name) := data
  // }
  axiReg<>io.axi
  io.axi.wlast := true.B

  // 组合逻辑解码
  val ctrl  = io.in.bits.ctrl
  val wdata = io.in.bits.rdata2 << (io.in.bits.result(1, 0) * 8.U)
  val wstrb = MuxLookup(ctrl.memLen, "b0000".U)(
    Seq(
      MemLen.BYTE -> ("b0001".U << io.in.bits.result(1, 0)),
      MemLen.HALF -> Mux(io.in.bits.result(1), "b1100".U, "b0011".U),
      MemLen.WORD -> "b1111".U
    )
  )

  val bytes       = VecInit.tabulate(4)(i => io.axi.rdata(8 * i + 7, 8 * i))
  val b           = bytes(io.in.bits.result(1, 0))
  val h           = Mux(io.in.bits.result(1), Cat(bytes(3), bytes(2)), Cat(bytes(1), bytes(0)))
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
  val outReg       = RegInit(0.U.asTypeOf(new EXU2LSU))
  val memRdataReg = RegInit(0.U(32.W))
  val memAddr     = io.in.bits.result
  object State extends ChiselEnum {
    val sIdle, sArWait, sAwWait, sRWait, sBWait, sOut = Value
  }
  val state = RegInit(State.sIdle)
  switch(state) {
    is(State.sIdle) {
      when(io.in.fire) {
        outReg := io.in.bits
        when(ctrl.memR) {
          axiReg.araddr  := memAddr
          axiReg.arvalid := true.B
          axiReg.arsize  := ctrl.memLen
          state          := State.sArWait
        }
          .elsewhen(ctrl.memWen) {
            axiReg.awaddr  := memAddr
            axiReg.awvalid := true.B
            axiReg.wdata   := wdata
            axiReg.wstrb   := wstrb
            axiReg.wvalid  := true.B
            axiReg.awsize  := ctrl.memLen
            state          := State.sAwWait
          }
          .otherwise {
            state := State.sOut
          }
      }
    }
    is(State.sArWait) {
      when(axiReg.arvalid && io.axi.arready) {
        axiReg.arvalid := false.B
        axiReg.rready  := true.B
        state          := State.sRWait
      }
    }
    is(State.sAwWait) {
      when(axiReg.awvalid && io.axi.awready) {
        axiReg.awvalid := false.B
        axiReg.wvalid  := false.B
        axiReg.bready  := true.B
        state          := State.sBWait
      }
    }
    is(State.sRWait) {
      when(io.axi.rvalid && axiReg.rready) {
        state         := State.sOut
        memRdataReg   := memReadData
        axiReg.rready := false.B
        when(io.axi.rresp(1)) {
          outReg.pc := 0.U
        }
      }
    }
    is(State.sBWait) {
      when(io.axi.bvalid && axiReg.bready) {
        state         := State.sOut
        axiReg.bready := false.B
        when(io.axi.bresp(1)){
          outReg.pc:=0.U
        }
      }
    }
    is(State.sOut) {
      when(io.out.fire) {
        state := State.sIdle
      }
    }
  }

  outReg.elements.foreach { case (name, data) =>
    if (io.out.bits.elements.contains(name))
      io.out.bits.elements(name) := data
  }
  io.out.bits.memRdata := memRdataReg

  io.out.valid := state === State.sOut
  io.in.ready  := state === State.sIdle
}
