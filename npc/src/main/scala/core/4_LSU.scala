package top

import chisel3._
import chisel3.util._
import ControlConstants._

class LoadStoreUnit extends Module {
  val io = IO(new Bundle {
    val in    = Flipped(Decoupled(new EXU2LSU))
    val out   = Decoupled(new LSU2WBU)
    val toMem = new AXI4LiteIO
  })

  val ctrl = io.in.bits.ctrl

  object State extends ChiselEnum {
    val sIdle, sWait = Value
  }
  val state = RegInit(State.sIdle)
  val memRdataReg  = RegInit(0.U(32.W))
  val memFinishReg = RegInit(false.B)

  val araddrReg  = RegInit(0.U(32.W))
  val arvalidReg = RegInit(false.B)
  val rreadyReg  = RegInit(false.B)
  val awaddrReg  = RegInit(0.U(32.W))
  val awvalidReg = RegInit(false.B)
  val wvalidReg  = RegInit(false.B)
  val breadyReg  = RegInit(false.B)

  io.toMem.araddr  := araddrReg
  io.toMem.arvalid := arvalidReg
  io.toMem.rready  := rreadyReg
  io.toMem.awaddr  := awaddrReg
  io.toMem.awvalid := awvalidReg
  io.toMem.wvalid  := wvalidReg
  io.toMem.bready  := breadyReg
  io.toMem.wdata   := io.in.bits.rdata2 << (io.in.bits.result(1, 0) * 8.U)
  io.toMem.wstrb   := MuxLookup(ctrl.memLen, "b0000".U)(
    Seq(
      LEN_BYTE -> ("b0001".U << io.in.bits.result(1, 0)),
      LEN_HALF -> Mux(io.in.bits.result(1), "b1100".U, "b0011".U),
      LEN_WORD -> "b1111".U
    )
  )

  val bytes       = VecInit.tabulate(4)(i => io.toMem.rdata(8 * i + 7, 8 * i))
  val b           = bytes(io.in.bits.result(1, 0))
  val h           = Mux(io.in.bits.result(1), Cat(bytes(3), bytes(2)), Cat(bytes(1), bytes(0)))
  val readByte    = Mux(ctrl.memSext, Cat(Fill(24, b(7)), b), Cat(0.U(24.W), b))
  val readHalf    = Mux(ctrl.memSext, Cat(Fill(16, h(15)), h), Cat(0.U(16.W), h))
  val memReadData = MuxLookup(ctrl.memLen, io.toMem.rdata)(
    Seq(
      LEN_BYTE -> readByte,
      LEN_HALF -> readHalf,
      LEN_WORD -> io.toMem.rdata
    )
  )

  val isLS = ctrl.memR || ctrl.memWen
  memFinishReg := false.B

  switch(state) {
    // 空闲状态:等待新的有效输入
    is(State.sIdle) {
      when(io.in.valid && isLS && !memFinishReg) {
        arvalidReg := !ctrl.memWen
        awvalidReg := ctrl.memWen
        wvalidReg  := ctrl.memWen
        araddrReg  := io.in.bits.result
        awaddrReg  := io.in.bits.result
        when(ctrl.memWen) {
          when(io.toMem.awready && io.toMem.wready) {
            state      := State.sWait
            breadyReg  := true.B
            awvalidReg := false.B
            wvalidReg  := false.B
          }
        }.otherwise {
          when(io.toMem.arready) {
            state      := State.sWait
            rreadyReg  := true.B
            arvalidReg := false.B

          }
        }
      }
    }
    // 等待状态:等待内存读取完成
    is(State.sWait) {

      when(io.toMem.rvalid || io.toMem.bvalid) { // 读写响应握手
        state        := State.sIdle
        memRdataReg  := memReadData
        rreadyReg    := false.B
        breadyReg    := false.B
        memFinishReg := true.B
      }

    }
  }

  // 加入随机延迟
  // val arvalidDelay = Module(new RandomDelay(3))
  // val awvalidDelay = Module(new RandomDelay(4))
  // val wvalidDelay  = Module(new RandomDelay(5))
  // val rreadyDelay  = Module(new RandomDelay(4))
  // val breadyDelay  = Module(new RandomDelay(3))
  // arvalidDelay.io.trigger := false.B
  // awvalidDelay.io.trigger := false.B
  // wvalidDelay.io.trigger  := false.B
  // rreadyDelay.io.trigger  := false.B
  // breadyDelay.io.trigger  := false.B

  // switch(state) {
  //   is(State.sIdle) {
  //     when(io.in.valid && isLS && !memFinishReg) {
  //       arvalidDelay.io.trigger := !ctrl.memWen
  //       awvalidDelay.io.trigger := ctrl.memWen
  //       wvalidDelay.io.trigger  := ctrl.memWen

  //       araddrReg := io.in.bits.result
  //       awaddrReg := io.in.bits.result

  //       when(ctrl.memWen) {
  //         when(io.toMem.awready && io.toMem.wready && awvalidReg && wvalidReg) { // 写请求握手
  //           breadyDelay.io.trigger := true.B
  //         }
  //       }.otherwise {
  //         when(io.toMem.arready && arvalidReg) { // 读请求握手
  //           rreadyDelay.io.trigger := true.B
  //         }
  //       }
  //     }

  //     arvalidReg := arvalidReg || arvalidDelay.io.ready
  //     awvalidReg := awvalidReg || awvalidDelay.io.ready
  //     wvalidReg  := wvalidReg || wvalidDelay.io.ready

  //     when(!breadyReg) {
  //       when(breadyDelay.io.ready) {
  //         breadyReg := true.B
  //         state     := State.sWait
  //       }
  //     }
  //     when(!rreadyReg) {
  //       when(rreadyDelay.io.ready) {
  //         rreadyReg := true.B
  //         state     := State.sWait
  //       }
  //     }
  //   }

  //   is(State.sWait) {
  //     arvalidReg := false.B
  //     awvalidReg := false.B
  //     wvalidReg  := false.B

  //     when(io.toMem.rvalid || io.toMem.bvalid) { // 读写响应握手
  //       state        := State.sIdle
  //       memRdataReg  := memReadData
  //       rreadyReg    := false.B
  //       breadyReg    := false.B
  //       memFinishReg := true.B
  //     }
  //   }
  // }

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
