module InstFetchUnitExt (
    input [31:0] io_pc,
    output reg [31:0] io_inst,
    input io_clock,
    input io_reset,
    input io_reqValid,
    output reg io_respValid,
    output reg io_reqReady,
    input io_respReady

);

    import "DPI-C" function int mem_read(input int addr);

    parameter IDLE = 0, FETCH = 1, DELAY = 2;
    reg [1:0]state;
    wire resp_trigger, req_trigger,resp_delay_ready, req_delay_ready;
    VRandomDelay #(.DELAY_BITS(8)) u_resp_delay (
        .clock(io_clock),
        .reset(io_reset),
        .trigger(resp_trigger),
        .ready(resp_delay_ready)
    );
    VRandomDelay #(.DELAY_BITS(8)) u_req_delay (
        .clock(io_clock),
        .reset(io_reset),
        .trigger(req_trigger),
        .ready(req_delay_ready)
    );
    assign resp_trigger = (state == FETCH) && io_respReady;
    assign req_trigger  = (state == FETCH) && io_respReady;

    always @(posedge io_clock)begin
        if(io_reset)begin
            state<=IDLE;
            io_reqReady<=1;
        end
        else begin
            if (state==IDLE) begin
            
                io_respValid <= 0;
            if (io_reqValid) begin
                io_reqReady<=0;
                state <= FETCH;
            end
        end
        else if (state==FETCH) begin
            
            if(io_respReady) begin
                io_inst<= mem_read(io_pc);

                // io_respValid <= 1;
                // io_reqReady<=1;
                // state <= IDLE;

                state <= DELAY;

            end
        end
        else if (state==DELAY) begin
            if(!io_respValid) begin
                if(resp_delay_ready) begin
                    io_respValid <= 1;
                end
            end
            if(!io_reqReady) begin
                if(req_delay_ready) begin
                    io_reqReady <= 1;
                end
            end
            if (io_respValid && io_reqReady) begin
                state <= IDLE;
            end
        end
        end
    end
    



endmodule

