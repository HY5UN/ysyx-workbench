module top(
  input sw0,
  input sw1,
  output ld0
);
  assign ld0 = sw0 ^ sw1; 

endmodule
