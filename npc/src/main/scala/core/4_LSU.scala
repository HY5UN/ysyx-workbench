package top

import chisel3._
import chisel3.util._
import ControlConstants._

class LoadStoreUnit extends Module {
  val io     = IO(new Bundle {
    val in  = Flipped(Decoupled(new EXU2LSU))
    val out = Decoupled(new LSU2WBU)
    val axi = new AXI4IO
  })
  val axiReg = RegInit(0.U.asTypeOf(new AXI4Out))
  axiReg.elements.foreach { case (name, data) =>
    io.axi.elements(name) := data
  }

  val inReg = RegInit(0.U.asTypeOf(new EXU2LSU))

  val ctrl = io.in.bits.ctrl

  val memRdataReg = RegInit(0.U(32.W))

  // val araddrReg  = RegInit(0.U(32.W))
  // val arvalidReg = RegInit(false.B)
  // val rreadyReg  = RegInit(false.B)
  // val awaddrReg  = RegInit(0.U(32.W))
  // val awvalidReg = RegInit(false.B)
  // val wvalidReg  = RegInit(false.B)
  // val breadyReg  = RegInit(false.B)
  // val wdataReg   = RegInit(0.U(32.W))
  // val wstrbReg   = RegInit(0.U(4.W))

  // io.axi.araddr  := araddrReg
  // io.axi.arvalid := arvalidReg
  // io.axi.rready  := rreadyReg
  // io.axi.awaddr  := awaddrReg
  // io.axi.awvalid := awvalidReg
  // io.axi.wvalid  := wvalidReg
  // io.axi.bready  := breadyReg
  // io.axi.wdata   := wdataReg
  // io.axi.wstrb   := wstrbReg

  val wdata = io.in.bits.rdata2 << (io.in.bits.result(1, 0) * 8.U)
  val wstrb = MuxLookup(ctrl.memLen, "b0000".U)(
    Seq(
      LEN_BYTE -> ("b0001".U << io.in.bits.result(1, 0)),
      LEN_HALF -> Mux(io.in.bits.result(1), "b1100".U, "b0011".U),
      LEN_WORD -> "b1111".U
    )
  )

  val bytes       = VecInit.tabulate(4)(i => io.axi.rdata(8 * i + 7, 8 * i))
  val b           = bytes(io.in.bits.result(1, 0))
  val h           = Mux(io.in.bits.result(1), Cat(bytes(3), bytes(2)), Cat(bytes(1), bytes(0)))
  val readByte    = Mux(ctrl.memSext, Cat(Fill(24, b(7)), b), Cat(0.U(24.W), b))
  val readHalf    = Mux(ctrl.memSext, Cat(Fill(16, h(15)), h), Cat(0.U(16.W), h))
  val memReadData = MuxLookup(ctrl.memLen, io.axi.rdata)(
    Seq(
      LEN_BYTE -> readByte,
      LEN_HALF -> readHalf,
      LEN_WORD -> io.axi.rdata
    )
  )

  val memAddr = io.in.bits.result

  val isLS = ctrl.memR || ctrl.memWen

  object State extends ChiselEnum {
    val sIdle, sArWait, sAwWait, sRWait, sBWait, sOut = Value
  }
  val state = RegInit(State.sIdle)
  switch(state) {
    is(State.sIdle) {
      when(io.in.fire) {
        inReg := io.in.bits
        when(ctrl.memR) {
          axiReg.araddr  := memAddr
          axiReg.arvalid := true.B
          state          := State.sArWait
        }
          .elsewhen(ctrl.memWen) {
            axiReg.awaddr  := memAddr
            axiReg.awvalid := true.B
            axiReg.wdata   := wdata
            axiReg.wstrb   := wstrb
            axiReg.wvalid  := true.B
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
      }
    }
    is(State.sBWait) {
      when(io.axi.bvalid && axiReg.bready) {
        state         := State.sOut
        axiReg.bready := false.B
      }
    }
    is(State.sOut) {
      when(io.out.fire) {
        state := State.sIdle
      }
    }
  }

  io.out.bits.ctrl     := inReg.ctrl
  io.out.bits.result   := inReg.result
  io.out.bits.pc       := inReg.pc
  io.out.bits.imm      := inReg.imm
  io.out.bits.rd       := inReg.rd
  io.out.bits.rdata1   := inReg.rdata1
  io.out.bits.memRdata := memRdataReg

  io.out.valid := state === State.sOut
  io.in.ready  := state === State.sIdle
}
