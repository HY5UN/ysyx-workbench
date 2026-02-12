
module LoadStoreUnit (
    input io_clock,
    input [31:0] io_addr,
    input [31:0] io_wdata,
    output [31:0] io_rdata,
    input io_wen
);
    import "DPI-C" function bit [31:0] mem_read(input bit [31:0] addr);
    import "DPI-C" function void mem_write(input bit [31:0] addr, input bit [31:0] data);

    assign io_rdata = mem_read(io_addr);

    always @(posedge io_clock) begin
        if(io_wen) begin
            mem_write(io_addr, io_wdata);
        end
    end
    
endmodule