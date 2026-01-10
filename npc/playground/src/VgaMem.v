module VgaMem(
  input  [18:0] addr,
  output [23:0] data
);
  reg [23:0] vga_mem [0:327680];
  initial begin
    $readmemh("resource/2.hex", vga_mem);
  end
  assign data = vga_mem[addr];
endmodule
