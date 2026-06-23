module MemExt (
    input io_clock,
    input io_reset,

    input  [31:0] io_axi_araddr,
    input         io_axi_arvalid,
    output reg    io_axi_arready,
    input  [3:0]  io_axi_arid,       // 新增
    input  [7:0]  io_axi_arlen,      // 新增
    input  [2:0]  io_axi_arsize,     // 新增
    input  [1:0]  io_axi_arburst,    // 新增

    output reg [31:0] io_axi_rdata,
    output reg [1:0]  io_axi_rresp,
    output reg        io_axi_rvalid,
    output            io_axi_rlast,   // 新增，驱动为 0
    output     [3:0]  io_axi_rid,     // 新增，驱动为 0
    input             io_axi_rready,

    input  [31:0] io_axi_awaddr,
    input         io_axi_awvalid,
    output reg    io_axi_awready,
    input  [3:0]  io_axi_awid,       // 新增
    input  [7:0]  io_axi_awlen,      // 新增
    input  [2:0]  io_axi_awsize,     // 新增
    input  [1:0]  io_axi_awburst,    // 新增

    input  [31:0] io_axi_wdata,
    input  [3:0]  io_axi_wstrb,
    input         io_axi_wvalid,
    input         io_axi_wlast,      // 新增
    output reg    io_axi_wready,

    output reg [1:0] io_axi_bresp,
    output reg       io_axi_bvalid,
    output     [3:0] io_axi_bid,     // 新增，驱动为 0
    input            io_axi_bready
);

    // 不支持 burst，输出固定为 0
    assign io_axi_rlast = 1'b0;
    assign io_axi_rid   = 4'h0;
    assign io_axi_bid   = 4'h0;
    import "DPI-C" function int  mem_read(input int addr);
    import "DPI-C" function void mem_write(input int addr, input int data, input byte wmask);


    parameter IDLE = 0, FETCH = 1, DELAY = 2;
    reg [1:0] state;
    reg wen;
    reg wsuccess, rsuccess;

    always @(posedge io_clock)begin
        if(io_reset)begin
            state<=IDLE;
            io_axi_arready <= 1;
            io_axi_awready <= 1;    
            io_axi_wready <= 1;
            io_axi_rvalid <= 0;
            io_axi_bvalid <= 0;
            wen <= 0;
            wsuccess <= 0;
            rsuccess <= 0;
        end
        else begin
            if(state==IDLE) begin
                io_axi_rvalid <= 0;
                io_axi_bvalid <= 0;
                if(io_axi_arvalid)begin // 读请求通道握手
                    state <= FETCH;
                    io_axi_arready <= 0;
                    wen <= 0;
                end
                else if(io_axi_awvalid && io_axi_wvalid)begin // 写请求通道握手
                    state <= FETCH;
                    io_axi_awready <= 0;
                    io_axi_wready <= 0;
                    wen <= 1;
                end
            end
            else if (state==FETCH)begin

                if(wen) begin

                    if(!wsuccess) mem_write(io_axi_awaddr,io_axi_wdata,{4'b0,io_axi_wstrb});
                    io_axi_bvalid <= 1;
                    wsuccess <= 1;
                    if(io_axi_bready)begin// 写响应握手
                        state <= IDLE;
                        io_axi_awready <= 1;
                        io_axi_wready <= 1;       
                        wsuccess <= 0;                
                    end

                end
                else begin 
                    if(!rsuccess) io_axi_rdata<=mem_read(io_axi_araddr);
                    io_axi_rvalid <= 1;
                    rsuccess <= 1;
                    if(io_axi_rready)begin // 读响应握手
                        state <= IDLE;
                        io_axi_arready <= 1;
                        rsuccess <= 0;
                    end
                end

            end

        end
    end


    
endmodule   

