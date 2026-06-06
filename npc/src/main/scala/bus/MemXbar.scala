package top
import chisel3._
import chisel3.util._

class MemXbar extends Module {
  val io = IO(new Bundle {
    val s     = new AXI4LiteIO
    val mUART = Flipped(new AXI4LiteIO)
    val mRAM  = Flipped(new AXI4LiteIO)
  })

  val tie0 = Module(new AXI4LiteTie0)
  tie0.io.m <> io.s
  tie0.io.s <> io.mUART
  tie0.io.s <> io.mRAM

  val UART = AddressSpace(0x10000000L, 0x4L)
  val RAM  = AddressSpace(0x80000000L, 1024 * 1024 * 64 * 8L)

  object State extends ChiselEnum {
    val sIdle, sMUART, sMRAM = Value
  }
  val state = RegInit(State.sIdle)

  val isRead = io.s.arvalid
  val valid  = io.s.arvalid || io.s.awvalid || io.s.wvalid
  val addr   = Mux(isRead, io.s.araddr, io.s.awaddr)

  switch(state) {
    is(State.sIdle) {
      when(valid) {
        when(UART.contains(addr)) {
          state := State.sMUART
        }
          // .elsewhen(RAM.contains(addr)) {
          //   state := State.sMRAM
          // }
          .otherwise {
            
            state := State.sIdle
          }
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
  }

}

case class AddressSpace(base: Long, size: Long) {
  def contains(addr: UInt): Bool =
    addr >= base.U && addr < (base + size).U

  def end: Long = base + size
}
