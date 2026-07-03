package top

import chisel3._
import chisel3.util._

class Ifu2Icache extends Bundle {
  val pc        = Input(UInt(32.W))
  val inst      = Output(UInt(32.W))
  val fencei    = Input(Bool())
  val err       = Output(Bool())
  val pcValid   = Input(Bool())
  val instValid = Output(Bool())
}

class ICacheBlock(blockSizeB: Int) extends Bundle {
  val tag  = UInt()
  val data = Vec(blockSizeB / 4, UInt(32.W))
}

class ICache(cacheSizeB: Int = 32, blockSizeB: Int = 4, assoc: Int = 1) extends Module {
  val io = IO(new Bundle {
    val axi  = new AXI4IO
    val ifu  = new Ifu2Icache
    val miss = Output(Bool())
  })
  ChiselUtils.driveZeroOutputs(io.axi)
  io.miss := 0.U
  require(isPow2(assoc), "PLRU 实现要求 assoc 为 2 的幂")
  require(cacheSizeB % blockSizeB % assoc == 0, "cacheSizeB must be a multiple of blockSizeB and assoc")
  val numBlocks     = cacheSizeB / blockSizeB
  val numGroups     = numBlocks / assoc
  val wordsPerBlock = blockSizeB / 4

  val offsetLen = log2Ceil(blockSizeB)
  val indexLen  = log2Ceil(numGroups)

  val offset = if (offsetLen > 2) io.ifu.pc(offsetLen - 1, 2) else 0.U
  val index  = if (indexLen > 0) io.ifu.pc(offsetLen + indexLen - 1, offsetLen) else 0.U
  val tag    = io.ifu.pc(31, offsetLen + indexLen)

  val cache    = Reg(Vec(numGroups, Vec(assoc, new ICacheBlock(blockSizeB))))
  val validArr = RegInit(VecInit(Seq.fill(numGroups)(VecInit(Seq.fill(assoc)(false.B)))))
  when(io.ifu.fencei) {
    validArr := VecInit(Seq.fill(numGroups)(VecInit(Seq.fill(assoc)(false.B))))
  }

  val wayHitsOH = (0 until assoc).map(i => validArr(index)(i) && cache(index)(i).tag === tag)
  val wayDatas  = (0 until assoc).map(i => cache(index)(i).data(offset))

  val hit = VecInit(wayHitsOH).asUInt.orR
  io.ifu.inst := Mux1H(wayHitsOH, wayDatas)

  // 替换策略
  val plruBits   =
    if (assoc > 1)
      Some(RegInit(VecInit(Seq.fill(numGroups)(VecInit(Seq.fill(assoc - 1)(false.B))))))
    else None
  val wayHitIdx  = OHToUInt(VecInit(wayHitsOH))
  val replaceWay = Wire(UInt())
  if (assoc == 1) {
    replaceWay := 0.U
  } else {
    replaceWay := PLRU.victim(plruBits.get(index))
  }

  // 状态机
  object State extends ChiselEnum {
    val sIdle, sArWait, sRWait, sOut = Value
  }
  val state = RegInit(State.sIdle)
  val refillOffset = Reg(UInt(offset.getWidth.W))

  val err = RegInit(false.B)

  io.axi.arburst := "b01".U  // INCR
  io.axi.arsize  := "b010".U // 4字节
  io.axi.araddr  := Cat(io.ifu.pc(31, offsetLen), 0.U(offsetLen.W))
  io.axi.arvalid := state === State.sArWait
  io.axi.arlen   := (wordsPerBlock - 1).U
  io.axi.rready  := state === State.sRWait

  io.ifu.instValid := false.B
  io.ifu.err       := err
  io.axi.arvalid   := state === State.sArWait
  io.axi.rready    := state === State.sRWait

  switch(state) {
    is(State.sIdle) {
        when(hit) {
          if (assoc > 1) PLRU.access(plruBits.get(index), wayHitIdx)
          io.ifu.instValid := true.B
        }.elsewhen(io.ifu.pcValid) {
          io.miss                     := true.B
          refillOffset                := 0.U
          validArr(index)(replaceWay) := false.B
          state                       := State.sArWait
        }
    }
    is(State.sArWait) {
      when(io.axi.arready) {
        state := State.sRWait
      }
    }
    is(State.sRWait) {
      when(io.axi.rvalid) {
        cache(index)(replaceWay).tag                := tag
        cache(index)(replaceWay).data(refillOffset) := io.axi.rdata
        refillOffset                                := refillOffset + 1.U
        when(io.axi.rlast) {
          validArr(index)(replaceWay) := true.B
          if (assoc > 1) PLRU.access(plruBits.get(index), replaceWay)
          state                       := State.sOut
        }
        when(io.axi.rresp =/= 0.U) {
          err := true.B
        }
      }
    }
    is(State.sOut) {
      io.ifu.instValid:=true.B
      when(io.ifu.pcValid) {
        err   := false.B
        state := State.sIdle
      }
    }
  }

}
