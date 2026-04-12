
// module MemExt (
//     input io_clock,
//     input io_rvalid,
//     input [31:0] io_addr,
//     input [31:0] io_wdata,
//     output [31:0] io_rdata,
//     input io_wen,
//     input [3:0] io_wmask
// );
//     import "DPI-C" function int  mem_read(input int addr);
//     import "DPI-C" function void mem_write(input int addr, input int data, input byte wmask);

//     assign io_rdata = io_rvalid ? mem_read(io_addr) : 32'h0;

//     always @(posedge io_clock) begin
//         if(io_wen) begin
//             mem_write(io_addr, io_wdata, {4'b0, io_wmask});
//         end
//     end
    
// endmodule   

module MemExt (
    input         io_clock,
    input         io_rvalid,
    input  [31:0] io_addr,
    input  [31:0] io_wdata,
    output [31:0] io_rdata,
    input         io_wen,
    input  [3:0]  io_wmask
);
    // import "DPI-C" function int  mem_read(input int addr);
    // import "DPI-C" function void mem_write(input int addr, input int data, input byte wmask);
    // assign io_rdata = io_rvalid ? mem_read(io_addr) : 32'h0;
    // always @(posedge io_clock) begin
    //     if(io_wen) begin
    //         mem_write(io_addr, io_wdata, {4'b0, io_wmask});
    //     end
    // end

    wire [31:0] mem_rdata;

    Memory256x32 mem_mem (
        .clock  (io_clock),
        .wen    (io_wen),
        .wmask  (io_wmask),
        .addr   (io_addr[9:2]),
        .wdata  (io_wdata),
        .rdata  (mem_rdata)
    );

    assign io_rdata = io_rvalid ? mem_rdata : 32'h0;
endmodule

//yosys
// 256x32b (1KB) 同步写、异步读存储器模块
// 用于取指(IFU)和访存(MEM)各实例化一个，总计2KB
module Memory256x32 (
    input         clock,
    input         wen,
    input  [3:0]  wmask,
    input  [7:0]  addr,      // 256深度，字地址
    input  [31:0] wdata,
    output [31:0] rdata
);
    reg [31:0] mem [0:255];

    // 异步读：当前周期直接返回，保持单周期特性
    assign rdata = mem[addr];

    // 同步写，支持字节掩码
    always @(posedge clock) begin
        if (wen) begin
            if (wmask[0]) mem[addr][ 7: 0] <= wdata[ 7: 0];
            if (wmask[1]) mem[addr][15: 8] <= wdata[15: 8];
            if (wmask[2]) mem[addr][23:16] <= wdata[23:16];
            if (wmask[3]) mem[addr][31:24] <= wdata[31:24];
        end
    end
endmodule