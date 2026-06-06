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
      }
    }
    is(State.sBusy) {
      // printf("%c", io.axi.wdata(7, 0))
      writer.io.data := io.axi.wdata(7, 0)
      writer.io.enable := true.B
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
    val enable = Input(Bool())
  })
  setInline("WriteChar.v",
    """module WriteChar(
      |  input [7:0] data,
      |  input       enable
      |);
      |  import "DPI-C" function void dpic_skip_difftest_once();
      |
      |  always @(*) begin
      |    if (enable) begin
      |      $write("%c", data);
      |      dpic_skip_difftest_once();
      |    end
      |  end
      |endmodule
    """.stripMargin)
}