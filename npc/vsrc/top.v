module top(
  input clk,
  input rst,
  output reg [15:0] led;
);

  reg [31:0] cnt;
  always @(posedge clk) begin
    if(rst) begin led<=16'h0001;count<=0; end
    else begin
      if(cnt==32'd50000000) begin
        cnt<=0;
        led<={led[14:0],led[15]};
      end
      else cnt<=cnt+1;

  end
  end
  
endmodule
