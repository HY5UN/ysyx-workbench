module sCPUROM(
  input  [3:0]PC,
  output [7:0]inst
);
  reg [7:0] rom [0:15];
  initial begin
    $readmemh("resource/1-10.hex", rom);
  end
  assign inst = rom[PC];
endmodule
