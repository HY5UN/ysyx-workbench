package top
import chisel3._
import chisel3.util._
import chisel3.ExtModule

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
  writer.io.enable := false.B
  writer.io.data := 0.U


  switch(state) {
    is(State.sIdle) {
      bvalidReg := false.B
      when(io.axi.awvalid && io.axi.wvalid) {
        state      := State.sBusy
        awreadyReg := false.B
        wreadyReg  := false.B
        writer.io.enable := true.B
      }
    }
    is(State.sBusy) {
      // printf("%c", io.axi.wdata(7, 0))
      writer.io.data := io.axi.wdata(7, 0)
      writer.io.enable := false.B
      bvalidReg      := true.B
      when(io.axi.bready) {
        state      := State.sIdle
        awreadyReg := true.B
        wreadyReg  := true.B
      }
    }
  }

}



class WriteChar extends ExtModule {
  val io = IO(new Bundle {
    val data   = Input(UInt(8.W))
    val enable = Input(Bool())   // 写使能
  })
  setInline("WriteChar.v",
    """module WriteChar(
      |  input [7:0] io_data,
      |  input       io_enable
      |);
      |  always @(*) begin
      |    if (io_enable) $write("%c", io_data);
      |  end
      |endmodule
    """.stripMargin)
}
