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
    val sIdle, sWait,sFinish = Value
  }
  val state = RegInit(State.sIdle)
  val memRdataReg = RegInit(0.U(32.W))
  val memAddrReg  = RegInit(0.U(32.W))
  val memWenReg   = RegInit(false.B)

  // 写
  val mem = Module(new MemExt())
  mem.io.clock  := clock
  mem.io.rvalid := ctrl.memR
  // mem.io.addr   := memAddrReg
  mem.io.addr   := io.in.bits.result
  mem.io.wdata  := io.in.bits.rdata2 << (io.in.bits.result(1, 0) * 8.U)
  // mem.io.wen    := memWenReg
  mem.io.wen    := ctrl.memWen&&io.in.valid
  mem.io.wmask  := MuxLookup(ctrl.memLen, "b0000".U)(
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

  
  val hasRW = ctrl.memR || ctrl.memWen

  switch(state) {
    // 空闲状态:等待新的有效输入
    is(State.sIdle) {
      when(io.in.valid) {
        state      := State.sWait
        memAddrReg := io.in.bits.result
        memWenReg   := ctrl.memWen

      }
    }
    // 等待状态:等待内存读取完成
    is(State.sWait) {
      //当前SimpleBus要求地址有效一个周期后读写完成,因此这里等待一个周期后直接当作读写完成
      state       := State.sFinish
      when(ctrl.memR) {
        memRdataReg := memReadData
      }
      memWenReg   := false.B
    }
    is(State.sFinish) {
      state := State.sIdle
    }
  }

  io.out.bits.ctrl     := ctrl
  io.out.bits.result   := io.in.bits.result
  io.out.bits.pc       := io.in.bits.pc
  // io.out.bits.memRdata := memRdataReg
  io.out.bits.memRdata := memReadData
  io.out.bits.imm      := io.in.bits.imm
  io.out.bits.csrRdata := io.in.bits.csrRdata
  io.out.bits.rd       := io.in.bits.rd
  io.out.bits.rdata1   := io.in.bits.rdata1

  // io.out.valid := io.in.valid&&((state === State.sIdle && !hasRW) || (state === State.sFinish ))
  io.out.valid :=io.in.valid
  io.in.ready  := state === State.sIdle

}

class MemExt extends ExtModule {
  val io = IO(new Bundle {
    val clock  = Input(Clock())
    val rvalid = Input(Bool())
    val addr   = Input(UInt(32.W))
    val wdata  = Input(UInt(32.W))
    val wmask  = Input(UInt(4.W))
    val wen    = Input(Bool())
    val rdata  = Output(UInt(32.W))

  })
}
