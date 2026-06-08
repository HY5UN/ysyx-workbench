package top

import chisel3._
import chisel3.util._

class Arbiter extends Module {
  val io    = IO(new Bundle {
    val sIFU = Flipped(new AXI4IO)
    val sLSU = Flipped(new AXI4IO)
    val m    = new AXI4IO
  })
  val tie0m = Module(new AXI4MasterTie0())
  tie0m.io.m <> io.m
  val tie0s = Module(new AXI4SlaveTie0())
  tie0s.io.s <> io.sIFU
  tie0s.io.s <> io.sLSU

  object State extends ChiselEnum {
    val sIFU, sLSU = Value
  }
  val state = RegInit(State.sIFU)

  val sIFU_Finish = RegInit(false.B)
  val sLSU_Finish = RegInit(false.B)
  switch(state) {

    is(State.sIFU) {
      io.sIFU <> io.m
      when(io.sIFU.rready && io.m.rvalid){
        sIFU_Finish := true.B
      }
      when(sIFU_Finish){
        when(io.sLSU.arvalid||io.sLSU.awvalid||io.sLSU.wvalid){
          state := State.sLSU
          sIFU_Finish := false.B
        }
      }
    }
    is(State.sLSU) {
      io.sLSU <> io.m
      when(io.sLSU.rready && io.m.rvalid){
        sLSU_Finish := true.B
      }
      when(sLSU_Finish){
        when(io.sIFU.arvalid){
          state := State.sIFU
          sLSU_Finish := false.B
        }
      }
    }
  }
}
