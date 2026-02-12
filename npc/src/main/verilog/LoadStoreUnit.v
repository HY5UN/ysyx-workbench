
module LoadStoreUnit (
    input clock,
    input [31:0] addr,
    input [31:0] wdata,
    output [31:0] rdata,
    input wen
);
    import "DPI-C" function uint32_t mem_read(input uint32_t addr);
    import "DPI-C" function void mem_write(input uint32_t addr, input uint32_t data);

    assign rdata = mem_read(addr);

    always @(posedge clock) begin
        if(wen) begin
            mem_write(addr, wdata);
        end
    end
    
endmodule