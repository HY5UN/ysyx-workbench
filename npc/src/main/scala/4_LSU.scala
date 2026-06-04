package top
import chisel3._
import chisel3.util._
import ControlConstants._

class LoadStoreUnit extends Module {
  val io   = IO(new Bundle {
    val in  = Flipped(Decoupled(new EXU2LSU))
    val out = Decoupled(new LSU2WBU)
  })
  val ctrl = io.in.bits.ctrl

  object State extends ChiselEnum {
    val sIdle, sDelay, sWait = Value
  }
  val state = RegInit(State.sIdle)
  // val memRdataReg  = RegInit(0.U(32.W))
  // val memAddrReg   = RegInit(0.U(32.W))
  val memFinishReg = RegInit(false.B)
  // val reqValidReg  = RegInit(false.B)
  // val respReadyReg = RegInit(true.B)
  val araddrReg  = RegInit(0.U(32.W))
  val arvalidReg = RegInit(false.B)
  val rreadyReg  = RegInit(true.B)
  val awaddrReg  = RegInit(0.U(32.W))
  val awvalidReg = RegInit(false.B)
  val wvalidReg  = RegInit(false.B)
  val breadyReg  = RegInit(true.B)

  val reqValidDelay  = Module(new RandomDelay(2))
  val respReadyDelay = Module(new RandomDelay(4))
  reqValidDelay.io.trigger  := false.B
  respReadyDelay.io.trigger := false.B

  val mem = Module(new MemExt())
  mem.io.clock   := clock
  mem.io.reset   := reset
  mem.io.araddr  := araddrReg
  mem.io.arvalid := arvalidReg
  mem.io.rready  := rreadyReg
  mem.io.awaddr  := awaddrReg
  mem.io.awvalid := awvalidReg
  mem.io.wvalid  := wvalidReg
  mem.io.bready  := breadyReg
  // 写
  mem.io.wdata   := io.in.bits.rdata2 << (io.in.bits.result(1, 0) * 8.U)
  mem.io.wstrb   := MuxLookup(ctrl.memLen, "b0000".U)(
    Seq(
      LEN_BYTE -> ("b0001".U << io.in.bits.result(1, 0)),
      LEN_HALF -> Mux(io.in.bits.result(1), "b1100".U, "b0011".U),
      LEN_WORD -> "b1111".U
    )
  )
  // 读
  val bytes = VecInit.tabulate(4)(i => mem.io.rdata(8 * i + 7, 8 * i))
  val b           = bytes(io.in.bits.result(1, 0))
  val h           = Mux(io.in.bits.result(1), Cat(bytes(3), bytes(2)), Cat(bytes(1), bytes(0)))
  val readByte    = Mux(ctrl.memSext, Cat(Fill(24, b(7)), b), Cat(0.U(24.W), b))
  val readHalf    = Mux(ctrl.memSext, Cat(Fill(16, h(15)), h), Cat(0.U(16.W), h))
  val memReadData = MuxLookup(ctrl.memLen, mem.io.rdata)(
    Seq(
      LEN_BYTE -> readByte,
      LEN_HALF -> readHalf,
      LEN_WORD -> mem.io.rdata
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
          when(mem.io.awready && mem.io.wready) {
            state     := State.sWait
            breadyReg := true.B
          }
        }.otherwise {
          when(mem.io.arready) {
            state     := State.sWait
            rreadyReg := true.B
          }
        }
      }
    }
    is(State.sDelay) {
      // when(!reqValidReg) {
      //   reqValidReg := reqValidDelay.io.ready
      // }
      // when(!respReadyReg) {
      //   respReadyReg := respReadyDelay.io.ready
      // }
      // when(reqValidReg && respReadyReg) {
      //   state := State.sWait
      // }
    }
    // 等待状态:等待内存读取完成
    is(State.sWait) {
      // reqValidReg := false.B
      // when(mem.io.respValid) {
      //   state        := State.sIdle
      //   memRdataReg  := memReadData
      //   memWenReg    := false.B
      //   memFinishReg := true.B
      // }
      arvalidReg := false.B
      awvalidReg := false.B
      wvalidReg  := false.B
      when(mem.io.rvalid) {
        state        := State.sIdle
        memRdataReg  := memReadData
        rreadyReg    := false.B
        memFinishReg := true.B
      }
      when(mem.io.bvalid) {
        state        := State.sIdle
        breadyReg    := false.B
        memFinishReg := true.B
      }
    }

  }

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
class MemExt extends ExtModule {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())

    val araddr  = Input(UInt(32.W))
    val arvalid = Input(Bool())
    val arready = Output(Bool())

    val rdata  = Output(UInt(32.W))
    val rresp  = Output(UInt(2.W))
    val rvalid = Output(Bool())
    val rready = Input(Bool())

    val awaddr  = Input(UInt(32.W))
    val awvalid = Input(Bool())
    val awready = Output(Bool())

    val wdata  = Input(UInt(32.W))
    val wstrb  = Input(UInt(4.W))
    val wvalid = Input(Bool())
    val wready = Output(Bool())

    val bresp  = Output(UInt(2.W))
    val bvalid = Output(Bool())
    val bready = Input(Bool())

  })
}
