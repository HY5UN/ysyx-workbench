package top
import chisel3._
import chisel3.util._

class CLINT extends Module {
  val io = IO(new Bundle {
    val axi = Flipped(new AXI4IO)
  })
  ChiselUtils.driveZeroOutputs(io.axi)

  val mtime = RegInit(0.U(64.W))
  mtime        := mtime + 1.U
  io.axi.rdata := MuxLookup(io.axi.araddr , 0.U)(
    Seq(
      0x10000028.U -> mtime(31, 0),
      0x10000032.U -> mtime(63, 32)
    )
  )

  io.axi.rvalid  := true.B
  io.axi.arready := true.B
  io.axi.rlast   := true.B
}
