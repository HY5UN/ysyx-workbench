package top
import chisel3._
import chisel3.util._

class UART extends Module {
  val io    = IO(new Bundle {
    val axi = Flipped(new AXI4LiteIO)
  })
  object State extends ChiselEnum {
    val sIdle, sBusy = Value
  }
  val state = RegInit(State.sIdle)

  val tie0 = Module(new AXI4LiteTie0)
  tie0.io.s <> tie0.io.m
  io.axi <> tie0.io.s

  val awreadyReg = RegInit(true.B)
  val wreadyReg  = RegInit(true.B)
  val bvalidReg  = RegInit(false.B)
  io.axi.awready := awreadyReg
  io.axi.wready  := wreadyReg
  io.axi.bvalid  := bvalidReg

  val writer = Module(new WriteChar)

  switch(state) {
    is(State.sIdle) {
      bvalidReg := false.B
      when(io.axi.awvalid && io.axi.wvalid) {
        state      := State.sBusy
        awreadyReg := false.B
        wreadyReg  := false.B
      }
    }
    is(State.sBusy) {
      // printf("%c", io.axi.wdata(7, 0))
      writer.io.data := io.axi.wdata(7, 0)
      bvalidReg      := true.B
      when(io.axi.bready) {
        state      := State.sIdle
        awreadyReg := true.B
        wreadyReg  := true.B
      }
    }
  }

}

class WriteChar extends BlackBox {
  val io = IO(new Bundle {
    val data = Input(UInt(8.W))
  })
  setInline(
    "WriteChar.v",
    """module WriteChar(
      |  input [7:0] data
      |);
      |  always @(*) $write("%c", data);
      |endmodule
    """.stripMargin
  )
}
