package lab

import chisel3._
import chisel3.util._
import chisel3.experimental.{BlackBox, HasBlackBoxInline}

class VgaMem(val DEPTH: Int = 524288) extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val addr = Input(UInt(19.W))   // 524288 = 2^19
    val data = Output(UInt(24.W))
  })

  setInline("VgaMem.v",
    s"""
       |module VgaMem(
       |  input  [18:0] addr,
       |  output [23:0] data
       |);
       |  reg [23:0] vga_mem [0:${DEPTH - 1}];
       |  initial begin
       |    $$readmemh("resource/2.hex", vga_mem);
       |  end
       |  assign data = vga_mem[addr];
       |endmodule
       |""".stripMargin)
}