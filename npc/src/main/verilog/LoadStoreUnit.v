
module MemExt (
    input io_clock,
    input io_reset,
    input io_reqValid,
    output reg io_reqReady,
    output reg io_respValid,
    input io_respReady,
    input [31:0] io_addr,
    input [31:0] io_wdata,
    output reg [31:0] io_rdata,
    input io_wen,
    input [3:0] io_wmask
);
    import "DPI-C" function int  mem_read(input int addr);
    import "DPI-C" function void mem_write(input int addr, input int data, input byte wmask);

    wire resp_trigger, req_trigger,resp_delay_ready, req_delay_ready;

    VRandomDelay #(.DELAY_BITS(4)) u_resp_delay (
        .clock(io_clock),
        .reset(io_reset),
        .trigger(resp_trigger),
        .ready(resp_delay_ready)
    );
    VRandomDelay #(.DELAY_BITS(4)) u_req_delay (
        .clock(io_clock),
        .reset(io_reset),
        .trigger(req_trigger),
        .ready(req_delay_ready)
    );
    assign resp_trigger = (state == FETCH) && io_respReady;
    assign req_trigger  = (state == FETCH) && io_respReady;

    parameter IDLE = 0, FETCH = 1, DELAY = 2;
    reg [1:0] state;

    always @(posedge io_clock)begin
        if(io_reset)begin
            state<=IDLE;
            io_reqReady<=1;
        end
        else begin
            if(state==IDLE) begin
                io_respValid<=0;

            if(io_reqValid)begin
                io_reqReady<=0;
                state<=FETCH;
            end

        end
        else if (state==FETCH)begin
            
            if(io_respReady)begin
                if(io_wen)begin
                    mem_write(io_addr,io_wdata,{4'b0,io_wmask});

                end
                else begin
                    io_rdata<=mem_read(io_addr);
                end


                // io_respValid<=1;
                // io_reqReady<=1;
                // state<=IDLE;
                state<=DELAY;
            end


        end
        else if(state==DELAY)begin
            if(!io_respValid)begin
                if(resp_delay_ready)begin
                    io_respValid<=1;
                end
            end
            if(!io_reqReady)begin
                if(req_delay_ready)begin
                    io_reqReady<=1;
                end
            end
            if(io_respValid && io_reqReady)begin
                state<=IDLE;
            end

        end
        end
    end


    
endmodule   

