package top

import chisel3._
import chisel3.util._

class InstFetchUnit extends Module {
  val io = IO(new Bundle {
    val out            = Decoupled(new IFU2IDU)
    val in             = Flipped(Decoupled(new WBU2IFU))
    val axi            = new AXI4IO
    val pfm_icache_hit = Output(Bool())
  })

  val axiTie0m = Module(new AXI4MasterTie0)
  axiTie0m.io.m <> io.axi
  io.axi.arsize  := "b010".U // 取指固定32bit = 4字节
  io.axi.arburst := "b01".U  // INCR

  val outInstReg = RegInit(0.U(32.W))
  val outPcReg   = RegInit(0.U(32.W))

  // val araddrReg  = RegInit("h80000000".U(32.W))
  val araddrReg  = RegInit("h30000000".U(32.W))
  val arvalidReg = RegInit(false.B)
  val rreadyReg  = RegInit(false.B)
  io.axi.araddr  := araddrReg
  io.axi.arvalid := arvalidReg
  io.axi.rready  := rreadyReg

  // val icache = Module(new ICache(4, 128))
  // icache.io.pc      := io.in.bits.nextPC
  // icache.io.wen     := false.B
  // icache.io.wdata   := 0.U
  io.pfm_icache_hit := false.B

  object State extends ChiselEnum {
    val sInit, sIdle, sArWait, sRWait, sOut = Value
  }
  val state = RegInit(State.sInit)
  switch(state) {
    is(State.sInit) {
      arvalidReg := true.B
      state      := State.sArWait
    }
    is(State.sIdle) {
      when(io.in.fire) {

        araddrReg  := io.in.bits.nextPC
        arvalidReg := true.B
        state      := State.sArWait

      }
    }
    is(State.sArWait) {
      when(arvalidReg && io.axi.arready) {
        arvalidReg := false.B
        rreadyReg  := true.B
        state      := State.sRWait
      }
    }
    is(State.sRWait) {
      when(io.axi.rvalid && rreadyReg) {
        state      := State.sOut
        outInstReg := io.axi.rdata
        rreadyReg  := false.B
        outPcReg   := araddrReg
        when(io.axi.rresp(1)) {
          outPcReg := 0.U
        }

      }
    }
    is(State.sOut) {
      when(io.out.fire) {
        state := State.sIdle
      }
    }
  }

  io.out.valid := state === State.sOut
  io.in.ready  := state === State.sIdle

  io.out.bits.inst := outInstReg
  io.out.bits.pc   := outPcReg
}

class ICacheBlock(blockSizeB: Int) extends Bundle {
  val tag   = UInt()
  val data  = Vec(blockSizeB / 4, UInt(32.W))
}

class ICache(cacheSizeB: Int = 32, blockSizeB: Int = 4, assoc: Int = 1) extends Module {
  val io        = IO(new Bundle {
    val pc    = Input(UInt(32.W))
    val hit   = Output(Bool())
    val rdata = Output(UInt(32.W))
  })
  val numBlocks = cacheSizeB / blockSizeB
  val numGroups = numBlocks / assoc

  val offsetLen = log2Ceil(blockSizeB)
  val indexLen  = log2Ceil(numGroups)
  
  val offset = if(offsetLen > 2) io.pc(offsetLen - 1, 2) else 0.U
  val index = if(indexLen > 0) io.pc(offsetLen + indexLen - 1, offsetLen) else 0.U
  val tag   = io.pc(31, offsetLen + indexLen)

  val cache = Reg(Vec(numGroups, Vec(assoc, new ICacheBlock(blockSizeB))))
  val validArr = RegInit(VecInit(Seq.fill(numGroups)(VecInit(Seq.fill(assoc)(false.B)))))
  

  val wayHitsOH = (0 until assoc).map(validArr(index)(_)&& cache(index))(_).tag === tag)
  val wayDatas = (0 until assoc).map(cache(index)(_).data(offset))

  io.hit :=wayHitsOH.orR
  io.rdata := Mux1H(wayHitsOH, wayDatas)
}
