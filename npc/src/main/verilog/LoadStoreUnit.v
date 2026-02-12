
module LoadStoreUnit (
    input io_clock,
    input [31:0] io_addr,
    input [31:0] io_wdata,
    output [31:0] io_rdata,
    input io_wen,
    input [3:0] io_wmask
);
    import "DPI-C" function int  mem_read(input int addr);
    import "DPI-C" function void mem_write(input int addr, input int data, input byte wmask);

    assign io_rdata = mem_read(io_addr);

    always @(posedge io_clock) begin
        if(io_wen) begin
            mem_write(io_addr, io_wdata, {4'b0, io_wmask});
        end
    end
    
endmodule