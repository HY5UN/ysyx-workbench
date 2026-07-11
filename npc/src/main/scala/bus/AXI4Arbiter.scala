package top

import chisel3._
import chisel3.util._

class AXI4Arbiter extends Module {
  val io    = IO(new Bundle {
    val sIFU = Flipped(new AXI4IO)
    val sLSU = Flipped(new AXI4IO)
    val m    = new AXI4IO
  })
  DriveZeroSinks(io)

  object State extends ChiselEnum {
    val sIFU, sLSU = Value
  }
  val state = RegInit(State.sIFU)

  val sIFU_Finish = RegInit(false.B)
  val sLSU_Finish = RegInit(false.B)
  //读通道仲裁
  switch(state) {

    is(State.sIFU) {
      io.sIFU <> io.m
      when(io.sIFU.arvalid){
        sIFU_Finish := false.B
      }
      when(io.sIFU.rready && io.m.rvalid && io.m.rlast){
        sIFU_Finish := true.B
      }
      when(sIFU_Finish && !io.sIFU.arvalid){
        when(io.sLSU.arvalid){
          io.m.arvalid := false.B
          io.sIFU.arready :=false.B

          state := State.sLSU
          sIFU_Finish := false.B
        }
      }
    }
    is(State.sLSU) {
      io.sLSU <> io.m
      when(io.sLSU.arvalid){
        sLSU_Finish := false.B
      }
      when(io.sLSU.rready && io.m.rvalid && io.m.rlast){
        sLSU_Finish := true.B
      }
      when(sLSU_Finish){
        when(io.sIFU.arvalid ){
          io.m.arvalid := false.B
          io.sLSU.arready :=false.B
          state := State.sIFU
          sLSU_Finish := false.B
        }
      }
    }
  }

  // 写通道只连接lsu
  io.sLSU.awready := io.m.awready
  io.m.awvalid := io.sLSU.awvalid
  io.m.awaddr  := io.sLSU.awaddr
  io.m.awid    := io.sLSU.awid
  io.m.awlen   := io.sLSU.awlen
  io.m.awsize  := io.sLSU.awsize
  io.m.awburst := io.sLSU.awburst

  io.sLSU.wready := io.m.wready
  io.m.wvalid := io.sLSU.wvalid
  io.m.wdata  := io.sLSU.wdata
  io.m.wstrb  := io.sLSU.wstrb
  io.m.wlast  := io.sLSU.wlast  

  io.m.bready := io.sLSU.bready
  io.sLSU.bvalid := io.m.bvalid
  io.sLSU.bresp  := io.m.bresp
  io.sLSU.bid    := io.m.bid

}
