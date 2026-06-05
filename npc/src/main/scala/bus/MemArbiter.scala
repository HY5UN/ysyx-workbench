package top
import chisel3._
import chisel3.util._

class MemArbiter extends Module {
  val io = IO(new Bundle {
    val s0 = Flipped(new AXI4LiteIO)
    val s1 = Flipped(new AXI4LiteIO)
    val m  = new AXI4LiteIO
  })
	
	//默认阻断valid信号
	io.m.arvalid := false.B
	io.m.awvalid := false.B
	io.m.wvalid  := false.B
	

  object State extends ChiselEnum {
    val sIdle, sS0, sS1 = Value
  }
  object Conn  extends ChiselEnum {
    val s0, s1 = Value
  }

  val state    = RegInit(State.sIdle)
  val prevConn = RegInit(Conn.s0)
  val s0Valid  = io.s0.arvalid
  val s1Valid  = io.s1.arvalid || io.s1.awvalid || io.s1.wvalid


  switch(state) {
    is(State.sIdle) {
      when(s0Valid && s1Valid) {
        state := Mux(prevConn === Conn.s0, State.sS1, State.sS0)
      }.elsewhen(s0Valid) {
        state := State.sS0
      }.elsewhen(s1Valid) {
        state := State.sS1
      }
    }
    is(State.sS0) {
			io.s0<>io.m
			when(io.s0.rvalid && io.m.rready) {
				state := State.sIdle
				prevConn := Conn.s0
			}
		}
		is(State.sS1) {
			io.s1<>io.m
			when((io.s1.rvalid && io.m.rready) || (io.s1.bvalid && io.m.bready)) {
				state := State.sIdle
				prevConn := Conn.s1
			}
		}

		}
  }

}
