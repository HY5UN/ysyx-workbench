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

    parameter IDLE = 0, FETCH = 1;
    reg state;
    //wire resp

    always @(posedge io_clock)begin
        if(io_reset)begin
            state<=IDLE;
            io_reqReady<=1;
        end
    end
    

    always @(posedge io_clock) begin
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
                io_respValid <= 1;
                io_reqReady<=1;
                state <= IDLE;
            end
        end
    end

endmodule

