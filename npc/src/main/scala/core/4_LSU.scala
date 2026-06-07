package top

import chisel3._
import chisel3.util._
import ControlConstants._

class LoadStoreUnit extends Module {
  val io = IO(new Bundle {
    val in    = Flipped(Decoupled(new EXU2LSU))
    val out   = Decoupled(new LSU2WBU)
    val memIO = new AXI4LiteIO
  })

  val inReadyReg  = RegInit(true.B)
  val outValidReg = RegInit(false.B)
  val inReg       = RegInit(0.U.asTypeOf(new EXU2LSU))

  val ctrl = io.in.bits.ctrl

  val memRdataReg = RegInit(0.U(32.W))

  val araddrReg  = RegInit(0.U(32.W))
  val arvalidReg = RegInit(false.B)
  val rreadyReg  = RegInit(false.B)
  val awaddrReg  = RegInit(0.U(32.W))
  val awvalidReg = RegInit(false.B)
  val wvalidReg  = RegInit(false.B)
  val breadyReg  = RegInit(false.B)
  val wdataReg   = RegInit(0.U(32.W))
  val wstrbReg   = RegInit(0.U(4.W))

  io.memIO.araddr  := araddrReg
  io.memIO.arvalid := arvalidReg
  io.memIO.rready  := rreadyReg
  io.memIO.awaddr  := awaddrReg
  io.memIO.awvalid := awvalidReg
  io.memIO.wvalid  := wvalidReg
  io.memIO.bready  := breadyReg
  io.memIO.wdata   := wdataReg
  io.memIO.wstrb   := wstrbReg

  val wdata = io.in.bits.rdata2 << (io.in.bits.result(1, 0) * 8.U)
  val wstrb = MuxLookup(ctrl.memLen, "b0000".U)(
    Seq(
      LEN_BYTE -> ("b0001".U << io.in.bits.result(1, 0)),
      LEN_HALF -> Mux(io.in.bits.result(1), "b1100".U, "b0011".U),
      LEN_WORD -> "b1111".U
    )
  )

  val bytes       = VecInit.tabulate(4)(i => io.memIO.rdata(8 * i + 7, 8 * i))
  val b           = bytes(io.in.bits.result(1, 0))
  val h           = Mux(io.in.bits.result(1), Cat(bytes(3), bytes(2)), Cat(bytes(1), bytes(0)))
  val readByte    = Mux(ctrl.memSext, Cat(Fill(24, b(7)), b), Cat(0.U(24.W), b))
  val readHalf    = Mux(ctrl.memSext, Cat(Fill(16, h(15)), h), Cat(0.U(16.W), h))
  val memReadData = MuxLookup(ctrl.memLen, io.memIO.rdata)(
    Seq(
      LEN_BYTE -> readByte,
      LEN_HALF -> readHalf,
      LEN_WORD -> io.memIO.rdata
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
        inReadyReg := false.B
        inReg      := io.in.bits
        when(ctrl.memR) {
          araddrReg  := memAddr
          arvalidReg := true.B
          state      := State.sArWait
        }
          .elsewhen(ctrl.memWen) {
            awaddrReg  := memAddr
            awvalidReg := true.B
            wdataReg   := wdata
            wstrbReg   := wstrb
            wvalidReg  := true.B
            state      := State.sAwWait
          }
          .otherwise {
            outValidReg := true.B
            state       := State.sOut
          }
      }
    }
    is(State.sArWait) {
      when(arvalidReg && io.memIO.arready) {
        arvalidReg := false.B
        rreadyReg  := true.B
        state      := State.sRWait
      }
    }
    is(State.sAwWait) {
      when(awvalidReg && io.memIO.awready) {
        awvalidReg := false.B
        wvalidReg  := false.B
        breadyReg  := true.B
        state      := State.sBWait
      }
    }
    is(State.sRWait) {
      when(io.memIO.rvalid && rreadyReg) {
        state       := State.sOut
        outValidReg := true.B
        memRdataReg := memReadData
        rreadyReg   := false.B
      }
    }
    is(State.sBWait) {
      when(io.memIO.bvalid && breadyReg) {
        state       := State.sOut
        outValidReg := true.B
        breadyReg   := false.B
      }
    }
    is(State.sOut) {
      when(io.out.fire) {
        state       := State.sIdle
        inReadyReg  := true.B
        outValidReg := false.B
      }
    }
  }

  // io.out.bits.ctrl     := ctrl
  // io.out.bits.result   := io.in.bits.result
  // io.out.bits.pc       := io.in.bits.pc
  io.out.bits.memRdata := memRdataReg
  // io.out.bits.imm      := io.in.bits.imm
  // io.out.bits.csrRdata := io.in.bits.csrRdata
  // io.out.bits.rd       := io.in.bits.rd
  // io.out.bits.rdata1   := io.in.bits.rdata1
  io.out.bits :<>= inReg

  io.out.valid := outValidReg
  io.in.ready  := inReadyReg
}
