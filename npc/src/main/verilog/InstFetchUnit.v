// module InstFetchUnitExt (
//     input [31:0] io_pc,
//     output [31:0] io_inst
// );

//     import "DPI-C" function int mem_read(input int addr);

//     assign io_inst = mem_read(io_pc);

// endmodule

module InstFetchUnitExt (
    input  [31:0] io_pc,
    output [31:0] io_inst
);
    // import "DPI-C" function int mem_read(input int addr);
    // assign io_inst = mem_read(io_pc);

    Memory256x32 ifu_mem (
        .clock  (1'b0),          // IFU只读，clock可接顶层clock，写端口悬空
        .wen    (1'b0),
        .wmask  (4'b0),
        .addr   (io_pc[9:2]),    // 字节地址→字地址，取[9:2]覆盖1KB空间
        .wdata  (32'b0),
        .rdata  (io_inst)
    );
endmodule