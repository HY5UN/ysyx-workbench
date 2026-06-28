package top

import chisel3._
import chisel3.util._

class AXI4Arbiter extends Module {
  val io    = IO(new Bundle {
    val sIFU = Flipped(new AXI4IO)
    val sLSU = Flipped(new AXI4IO)
    val m    = new AXI4IO
  })
  ChiselUtils.driveZeroOutputs(io)

  object State extends ChiselEnum {
    val sIFU, sLSU = Value
  }
  val state = RegInit(State.sIFU)

  val sIFU_Finish = RegInit(false.B)
  val sLSU_Finish = RegInit(false.B)
  switch(state) {

    is(State.sIFU) {
      io.sIFU <> io.m
      when(io.sIFU.rready && io.m.rvalid && io.m.rlast){
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
      when((io.sLSU.rready && io.m.rvalid && io.m.rlast)||(io.sLSU.bready && io.m.bvalid)){
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
