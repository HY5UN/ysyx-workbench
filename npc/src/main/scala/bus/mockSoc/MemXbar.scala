package top
import chisel3._
import chisel3.util._

class MemXbar extends Module {
  val io = IO(new Bundle {
    val s     = Flipped(new AXI4IO)
    val mUART = new AXI4IO
    val mRAM  = new AXI4IO
    val mCLINT = new AXI4IO
  })

  val tie0m = Module(new AXI4MasterTie0)
  val tie0s = Module(new AXI4SlaveTie0)
  tie0s.io.s <> io.s
  tie0m.io.m <> io.mUART
  tie0m.io.m <> io.mRAM
  tie0m.io.m <> io.mCLINT

  val UART = AddressSpace(0x10000000L, 0x4L)
  val RAM  = AddressSpace(0x80000000L, 1024 * 1024 * 64 * 8L)
  val CLINT = AddressSpace(0x10000028L,0x4L)

  object State extends ChiselEnum {
    val sIdle, sMUART, sMRAM, sCLINT = Value
  }
  val state = RegInit(State.sIdle)

  
  val isReadReg = RegInit(false.B)
  val validReg = RegInit(false.B)
  isReadReg := io.s.arvalid
  validReg  := io.s.arvalid || io.s.awvalid || io.s.wvalid
  val addr   = Mux(isReadReg, io.s.araddr, io.s.awaddr)

  switch(state) {
    is(State.sIdle) {
      when(validReg) {
        when(UART.contains(addr)) {
          state := State.sMUART
        }
          // .elsewhen(RAM.contains(addr)) {
          //   state := State.sMRAM
          // }
          .elsewhen(CLINT.contains(addr)) {
            state := State.sCLINT
          }
          .otherwise {
            state := State.sMRAM
          }
          validReg := false.B
      }
    }
    is(State.sMUART) {
      io.mUART <> io.s
      when((io.s.rvalid && io.mUART.rready) || (io.s.bvalid && io.mUART.bready)) {
        state := State.sIdle
      }
    }
    is(State.sMRAM) {
      io.mRAM <> io.s
      when((io.s.rvalid && io.mRAM.rready) || (io.s.bvalid && io.mRAM.bready)) {
        state := State.sIdle
      }
    }
    is(State.sCLINT) {
      io.mCLINT <> io.s
      when((io.s.rvalid && io.mCLINT.rready) || (io.s.bvalid && io.mCLINT.bready)) {
        state := State.sIdle
      }
    }
  }

}

case class AddressSpace(base: Long, size: Long) {
  def contains(addr: UInt): Bool =
    addr >= base.U && addr < (base + size).U

  def end: Long = base + size
}
