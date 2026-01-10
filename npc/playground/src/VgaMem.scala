package lab

import chisel3._
import chisel3.util.HasBlackBoxInline  // 注意：新版本里这个 trait 会被弃用，但 setInline 的能力 ExtModule 也有
import chisel3.experimental.ExtModule

class VgaMem extends ExtModule with HasBlackBoxInline {
  val addr = IO(Input(UInt(19.W)))
  val data = IO(Output(UInt(24.W)))

  setInline("VgaMem.v",
    """
      |module VgaMem(
      |  input  [18:0] addr,
      |  output [23:0] data
      |);
      |  reg [23:0] vga_mem [0:524287];
      |  initial begin
      |    $readmemh("resource/2.hex", vga_mem);
      |  end
      |  assign data = vga_mem[addr];
      |endmodule
      |""".stripMargin)
}
