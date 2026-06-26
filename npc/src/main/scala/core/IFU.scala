package top

import chisel3._
import chisel3.util._

class Ifu2Icache extends Bundle {
  val pc   = Input(UInt(32.W))
  val inst = Output(UInt(32.W))
}

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

  val icache = Module(new ICache(32, 4, 1))
  icache.io.axi <> io.axi
  icache.io.ifu.pc  := araddrReg
  icache.io.ifu.valid :=  state===State.sFetch
  icache.io.ifu.ready := state===State.sFetch 
  pfm_icache_hit :=false.B
  

  object State extends ChiselEnum {
    val sInit, sIdle, sFetch, sOut = Value
  }
  val state = RegInit(State.sInit)
  switch(state) {
    is(State.sInit) {
      state:= State.sFetch
    }
    is(State.sIdle) {
      when(io.in.fire) {

        araddrReg  := io.in.bits.nextPC
        state      := State.sFetch

      }
    }
    is(State.sFetch) {
      when(icache.io.ifu.valid) {
        outInstReg := icache.io.ifu.inst
        outPcReg   := araddrReg
        state      := State.sOut
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
  val tag  = UInt()
  val data = Vec(blockSizeB / 4, UInt(32.W))
}

class ICache(cacheSizeB: Int = 32, blockSizeB: Int = 4, assoc: Int = 1) extends Module {
  val io        = IO(new Bundle {
    val axi = new AXI4IO
    val ifu = Decoupled(new Ifu2Icache)
  })
  require(cacheSizeB % blockSizeB % assoc == 0, "cacheSizeB must be a multiple of blockSizeB and assoc")
  val numBlocks = cacheSizeB / blockSizeB
  val numGroups = numBlocks / assoc

  val offsetLen = log2Ceil(blockSizeB)
  val indexLen  = log2Ceil(numGroups)

  val offset = if (offsetLen > 2) io.ifu.pc(offsetLen - 1, 2) else 0.U
  val index  = if (indexLen > 0) io.ifu.pc(offsetLen + indexLen - 1, offsetLen) else 0.U
  val tag    = io.ifu.pc(31, offsetLen + indexLen)

  val cache     = Reg(Vec(numGroups, Vec(assoc, new ICacheBlock(blockSizeB))))
  val validArr  = RegInit(VecInit(Seq.fill(numGroups)(VecInit(Seq.fill(assoc)(false.B)))))
  val wayHitsOH = (0 until assoc).map(i => validArr(index)(i) && cache(index)(i).tag === tag)
  val wayDatas  = (0 until assoc).map(i => cache(index)(i).data(offset))

  val hit = VecInit(wayHitsOH).asUInt.orR
  io.ifu.inst := Mux1H(wayHitsOH, wayDatas)

  object State extends ChiselEnum {
    val sIdle, sArWait, sRWait, sOut = Value
  }
  val state = RegInit(State.sIdle)

  switch(state) {
    is(State.sIdle) {
      when(io.ifu.fire) {
        when(hit) {
          state := State.sOut
        }.otherwise {
          state := State.sArWait
        }
      }
    }
    is(State.sArWait) {
      when(io.axi.arvalid && io.axi.arready){
        state := State.sRWait
      }
    }
    is(State.sRWait) {
      
    }
      
    is(State.sOut) {
      when(io.ifu.fire) {
        state := State.sIdle
      }
    }
  }

  io.ifu.ready := state === State.sIdle
  io.ifu.valid := state === State.sOut
  io.axi.arvalid := state === State.sArWait
  io.axi.rready := state === State.sRWait
}
