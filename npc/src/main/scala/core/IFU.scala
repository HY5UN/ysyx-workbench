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
        // when(icache.io.hit) {
        //   outInstReg        := icache.io.rdata
        //   outPcReg          := io.in.bits.nextPC
        //   state             := State.sOut
        //   io.pfm_icache_hit := true.B
        // }.otherwise {
          araddrReg  := io.in.bits.nextPC
          arvalidReg := true.B
          state      := State.sArWait
        // }
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

        // icache.io.wen   := true.B
        // icache.io.wdata := io.axi.rdata
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

class ICacheLine() extends Bundle {
  val valid = Bool()
  val tag   = UInt()
  val data  = UInt()
}

class ICache(blockSizeBytes: Int = 4, numLines: Int = 16) extends Module {
  val io = IO(new Bundle {
    val pc    = Input(UInt(32.W))
    val rdata = Output(UInt(32.W))
    val hit   = Output(Bool())
    val wen   = Input(Bool())
    val wdata = Input(UInt(32.W))
  })
  require(blockSizeBytes == 4, "ICache only supports block size of 4 bytes")

  val icache = Reg(Vec(numLines, new ICacheLine))
  for (i <- 0 until numLines) {
    when(reset.asBool) {
      icache(i).valid := false.B
    }
  }
  val offsetBits = log2Ceil(blockSizeBytes)
  val indexBits = log2Ceil(numLines)
  val tagBits   = 32 - offsetBits - indexBits

  val offset = io.pc(offsetBits - 1, 0)
  val index  = io.pc(offsetBits + indexBits - 1, offsetBits)
  val tag    = io.pc(31, offsetBits + indexBits)

  io.rdata := icache(index).data
  io.hit   := icache(index).valid && icache(index).tag === tag
  // io.hit :=false.B //disable icache
  when(io.wen) {
    icache(index).valid := true.B
    icache(index).tag   := tag
    icache(index).data  := io.wdata
  }
}
