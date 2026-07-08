package top

import chisel3._
import chisel3.util._
class LSU2WBU extends EXU2LSU {
  val dpic_memAddr   = UInt(32.W)
  val dpic_memRdata  = UInt(32.W)
  val dpic_memWdata  = UInt(32.W)
  val dpic_memRValid = Bool()
  val dpic_memWValid = Bool()
}
class LSU     extends Module  {
  val io = IO(new Bundle {
    val in  = Flipped(Decoupled(new EXU2LSU))
    val out = Decoupled(new LSU2WBU)
    val axi = new AXI4IO
  })

  ChiselUtils.driveZeroOutputs(io.axi)
  io.out.valid := false.B
  io.in.ready  := false.B

  val dpic_rvalidReg = RegInit(false.B)
  val dpic_wvalidReg = RegInit(false.B)

  // 组合逻辑解码
  val in      = io.in.bits
  val ctrl    = in.ctrl
  val memAddr = in.result
  val wdata   = Wire(UInt(32.W))
  wdata := in.rdata2 << (memAddr(1, 0) * 8.U)
  val wstrb = MuxLookup(ctrl.memLen, "b0000".U)(
    Seq(
      MemLen.BYTE -> ("b0001".U << memAddr(1, 0)),
      MemLen.HALF -> Mux(memAddr(1), "b1100".U, "b0011".U),
      MemLen.WORD -> "b1111".U
    )
  )

  val bytes       = VecInit.tabulate(4)(i => io.axi.rdata(8 * i + 7, 8 * i))
  val b           = bytes(memAddr(1, 0))
  val h           = Mux(memAddr(1), Cat(bytes(3), bytes(2)), Cat(bytes(1), bytes(0)))
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

  val memRdataReg = Reg(0.U(32.W))
  object State extends ChiselEnum {
    val sIdle, sArWait, sAwWait, sRWait, sBWait, sOut = Value
  }
  val state = RegInit(State.sIdle)

  val awDone = RegInit(false.B)
  val wDone  = RegInit(false.B)
  when(io.axi.awvalid && io.axi.awready) {
    awDone := true.B
  }
  when(io.axi.wvalid && io.axi.wready) {
    wDone := true.B
  }

  val excTypeReg  = Reg(ExceptionType())
  val excValidReg = RegInit(false.B)

  io.axi.araddr  := memAddr
  io.axi.arvalid := state === State.sArWait
  io.axi.arsize  := ctrl.memLen
  io.axi.rready  := state === State.sRWait
  io.axi.arlen   := 0.U

  io.axi.awaddr  := memAddr
  io.axi.awvalid := state === State.sAwWait && !awDone
  io.axi.awlen   := 0.U
  io.axi.wdata   := wdata
  io.axi.wstrb   := wstrb
  io.axi.wvalid  := state === State.sAwWait && !wDone
  io.axi.awsize  := ctrl.memLen
  io.axi.bready  := state === State.sBWait
  io.axi.wlast   := true.B


  switch(state) {
    is(State.sIdle) {
      io.out.valid := io.in.valid
      io.in.ready  := true.B
      when(io.in.valid && !io.in.bits.ctrl.excValid) {
        when(io.in.bits.ctrl.memR) {
          io.out.valid := false.B
          io.in.ready  := false.B
          state        := State.sArWait
        }.elsewhen(io.in.bits.ctrl.memWen) {
          io.out.valid := false.B
          io.in.ready  := false.B
          state        := State.sAwWait
          wDone        := false.B
          awDone       := false.B
        }
      }
    }
    is(State.sArWait) {
      when(io.axi.arvalid && io.axi.arready) {
        state := State.sRWait
      }
    }
    is(State.sAwWait) {
      when((awDone || io.axi.awready) && (wDone || io.axi.wready)) {
        state := State.sBWait
      }
    }
    is(State.sRWait) {
      when(io.axi.rvalid && io.axi.rready) {
        state       := State.sOut
        memRdataReg := memReadData
        when(io.axi.rresp =/= 0.U) {
          excTypeReg  := ExceptionType.LoadAccessFault
          excValidReg := true.B
        }

        dpic_rvalidReg := true.B
      }
    }
    is(State.sBWait) {
      when(io.axi.bvalid && io.axi.bready) {
        state := State.sOut
        when(io.axi.bresp =/= 0.U) {
          excTypeReg  := ExceptionType.StoreAccessFault
          excValidReg := true.B
        }

        dpic_wvalidReg := true.B
      }
    }
    is(State.sOut) {
      io.in.ready  := true.B
      io.out.valid := io.in.valid // 这时候in不valid说明被冲刷了
      state        := State.sIdle
      excValidReg  := false.B

      dpic_rvalidReg := false.B
      dpic_wvalidReg := false.B
    }
  }

  BundleConnect(in, io.out.bits)
  when(ctrl.rdSel === RdSel.MEM) {
    io.out.bits.gprWdata := memRdataReg
  }
  io.out.bits.dpic_memAddr   := memAddr
  io.out.bits.dpic_memRdata  := memRdataReg
  io.out.bits.dpic_memWdata  := in.rdata2
  io.out.bits.dpic_memRValid := dpic_rvalidReg
  io.out.bits.dpic_memWValid := dpic_wvalidReg

  io.out.bits.ctrl.excType  := excTypeReg
  io.out.bits.ctrl.excValid := excValidReg
  when(ctrl.excValid) {
    io.out.bits.ctrl.excType  := ctrl.excType
    io.out.bits.ctrl.excValid := true.B
  }
}
