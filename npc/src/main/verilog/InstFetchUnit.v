module InstFetchUnitExt (
    input [31:0] io_pc,
    output reg [31:0] io_inst,
    input io_clock,
    input io_reqValid,
    output reg io_respValid,
    output reg io_reqReady,
    input io_respReady

);

    import "DPI-C" function int mem_read(input int addr);

    parameter IDLE = 0, FETCH = 1;
    reg state;
    // always @(posedge io_clock) begin
    //     if(io_reqValid) begin
    //         io_inst<= mem_read(io_pc);
    //         io_respValid <= 1;
    //     end
    //     else begin
    //         io_respValid <= 0;  
    //     end 
    // end

    always @(posedge io_clock) begin
        if (state==IDLE) begin
            
            if (io_reqValid) begin
                io_respValid <= 0;
                io_reqReady<=0;
                state <= FETCH;
            end
        end
        else if (state==FETCH) begin
            
            if(io_respReady) begin
                io_inst<= mem_read(io_pc);
                io_respValid <= 1;
                io_reqReady<=1;
                state <= IDLE;
            end
        end
    end

endmodule

// module InstFetchUnitExt (
//     input  [31:0] io_pc,
//     output [31:0] io_inst
// );
//     // import "DPI-C" function int mem_read(input int addr);
//     // assign io_inst = mem_read(io_pc);

//     Memory256x32 ifu_mem (
//         .clock  (1'b0),          // IFU只读，clock可接顶层clock，写端口悬空
//         .wen    (1'b0),
//         .wmask  (4'b0),
//         .addr   (io_pc[9:2]),    // 字节地址→字地址，取[9:2]覆盖1KB空间
//         .wdata  (32'b0),
//         .rdata  (io_inst)
//     );
// endmodule