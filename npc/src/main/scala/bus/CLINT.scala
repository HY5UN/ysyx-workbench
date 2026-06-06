package top
import chisel3._
import chisel3.util._

class CLINT extends Module {
  val io    = IO(new Bundle {
    val axi = Flipped(new AXI4LiteIO)
  })
  object State extends ChiselEnum {
    val sIdle, sBusy = Value
  }
  val state = RegInit(State.sIdle)

  val tie0       = Module(new AXI4LiteTie0)
  tie0.io.s <> tie0.io.m
  io.axi <> tie0.io.s
  val arreadyReg = RegInit(true.B)
  val rvalidReg  = RegInit(false.B)
  val rdataReg   = RegInit(0.U(32.W))
  io.axi.arready := arreadyReg
  io.axi.rvalid  := rvalidReg
  io.axi.rdata   := rdataReg
  val mtime = RegInit(0.U(64.W))
  //mtime := mtime + 1.U
  mtime := 0.U

  val araddr = RegInit(0.U(32.W))
  val addr = Cat(araddr(araddr.getWidth-1, 2), 0.U(2.W))

  switch(state) {
    is(State.sIdle) {
      rvalidReg := false.B
      when(io.axi.arvalid) {
        araddr     := io.axi.araddr
        state      := State.sBusy
        arreadyReg := false.B
      }
    }
    is(State.sBusy) {
      rvalidReg := true.B
      rdataReg  := MuxLookup(addr, 0.U)(
        Seq(
          0x10000028.U -> mtime(31, 0),
          0x10000032.U -> mtime(63, 32)
        )
      )
      when(io.axi.rready) {
        state      := State.sIdle
        arreadyReg := true.B
      }
    }
  }
}
