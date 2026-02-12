module InstFetchUnit (
    input [31:0] io_pc,
    output [31:0] io_inst
);

    import "DPI-C" function bit[31:0] mem_read(input bit[31:0] addr);

    assign io_inst = mem_read(io_pc);

endmodule