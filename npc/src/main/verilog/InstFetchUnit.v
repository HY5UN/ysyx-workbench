module InstFetchUnit (
    input [31:0] io_pc,
    output [31:0] io_inst
);

    import "DPI-C" function int mem_read(input int addr);

    assign io_inst = mem_read(io_pc);

endmodule