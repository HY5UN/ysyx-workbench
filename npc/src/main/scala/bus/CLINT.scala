package top
import chisel3._
import chisel3.util._

class CLINT extends Module {
  val io = IO(new Bundle {
    val lsu = Flipped(new AXI4IO)
    val out = new AXI4IO
  })
  io.lsu <> io.out

  val isClint = io.lsu.arvalid && io.lsu.araddr(31, 16) === "h0200".U

  val connect = RegInit(false.B)
  when(isClint) {
    connect := true.B
  }
  when(io.lsu.rready) {
    connect := false.B
  }

  val mtime = RegInit(0.U(64.W))

  mtime := mtime + 1.U

  when(connect || isClint) {
    io.out.arvalid := false.B
    io.lsu.rdata   := Mux(io.lsu.araddr(15, 0) === "hbff8".U, mtime(31, 0), mtime(63, 32))

    io.lsu.arready := true.B
    io.lsu.rvalid  := true.B
    io.lsu.rlast   := true.B

  }
}
