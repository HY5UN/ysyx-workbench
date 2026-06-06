package top
import chisel3._
import chisel3.util._

class MemArbiter extends Module {
  val io = IO(new Bundle {
    val s0 = Flipped(new AXI4LiteIO)
    val s1 = Flipped(new AXI4LiteIO)
    val m  = new AXI4LiteIO
  })

  val tie0 = Module(new AXI4LiteTie0)
  tie0.io.s <> io.s0
  tie0.io.s <> io.s1
  tie0.io.m <> io.m

  object State extends ChiselEnum {
    val sIdle, sS0, sS1 = Value
  }

  val state   = RegInit(State.sS0)
  val s0Valid = io.s0.arvalid
  val s1Valid = io.s1.arvalid || io.s1.awvalid || io.s1.wvalid

  val s0Finish = RegInit(false.B)
  val s1Finish = RegInit(false.B)

  switch(state) {
    is(State.sS0) {
      io.s0 <> io.m
      when(io.s0.rvalid && io.m.rready) {
        s0Finish := true.B
      }
      when(s0Finish) {
        when(s1Valid) {
          state    := State.sS1
          s0Finish := false.B
        }

      }
    }
    is(State.sS1) {
      io.s1 <> io.m
      when((io.s1.rvalid && io.m.rready) || (io.s1.bvalid && io.m.bready)) {
        s1Finish := true.B
      }
      when(s1Finish) {
        when(s0Valid) {
          state    := State.sS0
          s1Finish := false.B
        }
      }
    }

  }

}
