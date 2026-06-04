module MemExt (
    input io_clock,
    input io_reset,
    
    input [31:0] io_araddr,
    input io_arvalid,
    output reg io_arready,

    output reg [31:0] io_rdata,
    output reg [1:0] io_rresp,
    output reg io_rvalid,
    input io_rready,

    input [31:0] io_awaddr,
    input io_awvalid,
    output reg io_awready,

    input [31:0] io_wdata,
    input [3:0] io_wstrb,
    input io_wvalid,
    output reg io_wready,

    output reg [1:0] io_bresp,
    output reg io_bvalid,
    input io_bready
);
    import "DPI-C" function int  mem_read(input int addr);
    import "DPI-C" function void mem_write(input int addr, input int data, input byte wmask);

    // wire resp_trigger, req_trigger,resp_delay_ready, req_delay_ready;

    // VRandomDelay #(.DELAY_BITS(4)) u_resp_delay (
    //     .clock(io_clock),
    //     .reset(io_reset),
    //     .trigger(resp_trigger),
    //     .ready(resp_delay_ready)
    // );
    // VRandomDelay #(.DELAY_BITS(4)) u_req_delay (
    //     .clock(io_clock),
    //     .reset(io_reset),
    //     .trigger(req_trigger),
    //     .ready(req_delay_ready)
    // );
    // assign resp_trigger = (state == FETCH) && io_respReady;
    // assign req_trigger  = (state == FETCH) && io_respReady;

    parameter IDLE = 0, FETCH = 1, DELAY = 2;
    reg [1:0] state;
    reg wen;

    always @(posedge io_clock)begin
        if(io_reset)begin
            state<=IDLE;
            io_arready <= 1;
            io_awready <= 1;    
            io_wready <= 1;
            io_rvalid <= 0;
            io_bvalid <= 0;
            wen <= 0;
        end
        else begin
            if(state==IDLE) begin
                io_rvalid <= 0;
                io_bvalid <= 0;
                if(io_arvalid)begin // 读请求通道握手
                    state <= FETCH;
                    io_arready <= 0;
                    wen <= 0;
                end
                else if(io_awvalid && io_wvalid)begin // 写请求通道握手
                    state <= FETCH;
                    io_awready <= 0;
                    io_wready <= 0;
                    wen <= 1;
                end
            end
            else if (state==FETCH)begin

                if(wen) begin
                    mem_write(io_addr,io_wdata,{4'b0,io_wmask});
                    io_bvalid <= 1;
                    if(io_bready)begin// 写响应通道握手
                        state <= IDLE;
                        io_awready <= 1;
                        io_wready <= 1;                       
                    end

                end
                else begin 
                    io_rdata<=mem_read(io_addr);
                    io_rvalid <= 1;
                    if(io_rready)begin // 读响应通道握手
                        state <= IDLE;
                        io_arready <= 1;
                    end
                end

            end


            end
            // else if(state==DELAY)begin
            //     if(!io_respValid)begin
            //         if(resp_delay_ready)begin
            //             io_respValid<=1;
            //         end
            //     end
            //     if(!io_reqReady)begin
            //         if(req_delay_ready)begin
            //             io_reqReady<=1;
            //         end
            //     end
            //     if(io_respValid && io_reqReady)begin
            //         state<=IDLE;
            //     end

            // end
        end
    end


    
endmodule   

