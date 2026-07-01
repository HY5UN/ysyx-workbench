package top
import chisel3._
import chisel3.util._

class MemXbar extends Module {
  val io = IO(new Bundle {
    val s      = Flipped(new AXI4IO)
    val mUART  = new AXI4IO
    val mRAM   = new AXI4IO
    val mCLINT = new AXI4IO
  })
  ChiselUtils.driveZeroOutputs(io)

  val UART  = AddressSpace(0x10000000L, 0x4L)
  val RAM   = AddressSpace(0x80000000L, 1024 * 1024 * 64 * 8L)
  val CLINT = AddressSpace(0x10000028L, 0x4L)

  def connectW(m: Data, s: Data) = {
    m.awready := s.awready
    s.awvalid := m.awvalid
    s.awaddr  := m.awaddr
    s.awid    := m.awid
    s.awlen   := m.awlen
    s.awsize  := m.awsize
    s.awburst := m.awburst

    m.wready := s.wready
    s.wvalid := m.wvalid
    s.wdata  := m.wdata
    s.wstrb  := m.wstrb
    s.wlast  := m.wlast

    s.bready := m.bready
    m.bvalid := s.bvalid
    m.bresp  := s.bresp
    m.bid    := s.bid
  }

  def connectR(m: Data, s: Data) = {
    m.arready := s.arready
    s.arvalid := m.arvalid
    s.araddr  := m.araddr
    s.arid    := m.arid
    s.arlen   := m.arlen
    s.arsize  := m.arsize
    s.arburst := m.arburst

    s.rready := m.rready
    m.rvalid := s.rvalid
    m.rdata  := s.rdata
    m.rresp  := s.rresp
    m.rlast  := s.rlast
    m.rid    := s.rid
  }

  object State extends ChiselEnum {
    val sIdle, sUART, sRAM, sCLINT = Value
  }
  val state = RegInit(State.sIdle)

  val wstate = RegInit(State.sIdle)
  switch(wstate) {
    is(State.sIdle) {
      when(io.s.awvalid) {
        when(UART.contains(io.s.awaddr)) {
          wstate := State.sUART
        }.elsewhen(CLINT.contains(io.s.awaddr)) {
          wstate := State.sCLINT
        }.otherwise {
          wstate := State.sRAM

        }
      }
    }
    is(State.sUART) {
      connectW(io.s, io.mUART)
      when(io.s.bready && io.mUART.bvalid) {
        wstate := State.sIdle
      }
    }
    is(State.sCLINT) {
      connectW(io.s, io.mCLINT)
      when(io.s.bready && io.mCLINT.bvalid) {
        wstate := State.sIdle
      }
    }
    is(State.sRAM) {
      connectW(io.s, io.mRAM)
      when(io.s.bready && io.mRAM.bvalid) {
        wstate := State.sIdle
      }
    }
  }

  val rstate = RegInit(State.sIdle)
  switch(rstate) {
    is(State.sIdle) {
      when(io.s.arvalid) {
        when(UART.contains(io.s.araddr)) {
          rstate := State.sUART
        }.elsewhen(CLINT.contains(io.s.araddr)) {
          rstate := State.sCLINT
        }.otherwise {
          rstate := State.sRAM
        }
      }
    }
    is(State.sUART) {
      connectR(io.s, io.mUART)
      when(io.s.rready && io.mUART.rvalid && io.mUART.rlast) {
        rstate := State.sIdle
      }
    }
    is(State.sCLINT) {
      connectR(io.s, io.mCLINT)
      when(io.s.rready && io.mCLINT.rvalid && io.mCLINT.rlast) {
        rstate := State.sIdle
      }
    }
    is(State.sRAM) {
      connectR(io.s, io.mRAM)
      when(io.s.rready && io.mRAM.rvalid && io.mRAM.rlast) {
        rstate := State.sIdle
      }
    }
  }

}

case class AddressSpace(base: Long, size: Long) {
  def contains(addr: UInt): Bool =
    addr >= base.U && addr < (base + size).U

  def end: Long = base + size
}
