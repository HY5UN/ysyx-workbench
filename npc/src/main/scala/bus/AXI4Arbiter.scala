package top

import chisel3._
import chisel3.util._

class AXI4Arbiter extends Module {
  val io = IO(new Bundle {
    val sIFU = Flipped(new AXI4IO)
    val sLSU = Flipped(new AXI4IO)
    val m    = new AXI4IO
  })
  DriveZeroSinks(io)

  io.m.aw <> io.sLSU.aw
  io.m.w  <> io.sLSU.w
  io.m.b  <> io.sLSU.b

  
  val outstandingQueue = Module(new Queue(Bool(), entries = 4))

  val lsu_ar_req = io.sLSU.ar.valid
  val ifu_ar_req = io.sIFU.ar.valid
  val ar_allow   = outstandingQueue.io.enq.ready // FIFO 有空位才允许发起新请求

  
  io.m.ar.valid := (lsu_ar_req || ifu_ar_req) && ar_allow
  io.m.ar.addr  := Mux(lsu_ar_req, io.sLSU.ar.addr, io.sIFU.ar.addr)

  io.sLSU.ar.ready := io.m.ar.ready && ar_allow
  io.sIFU.ar.ready := io.m.ar.ready && !lsu_ar_req && ar_allow // LSU 优先，IFU 需等待 LSU 闲置

  val ar_fire = io.m.ar.valid && io.m.ar.ready
  outstandingQueue.io.enq.valid := ar_fire
  outstandingQueue.io.enq.bits  := lsu_ar_req

  
  val r_target_is_lsu = outstandingQueue.io.deq.bits
  val r_valid_real    = io.m.r.valid && outstandingQueue.io.deq.valid

  io.sLSU.r.valid := r_valid_real && r_target_is_lsu
  io.sLSU.r.data  := io.m.r.data

  io.sIFU.r.valid := r_valid_real && !r_target_is_lsu
  io.sIFU.r.data  := io.m.r.data

  io.m.r.ready := Mux(r_target_is_lsu, io.sLSU.r.ready, io.sIFU.r.ready)

  val r_fire = io.m.r.valid && io.m.r.ready
  outstandingQueue.io.deq.ready := r_fire && io.m.r.last
}