
module MemExt (
    input io_clock,
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

    //reg [1:0] delayCounter;

    parameter IDLE = 0, FETCH = 1;
    reg state;
    always @(posedge io_clock) begin
        if(state==IDLE) begin
            if(io_reqValid)begin
                io_respValid<=0;
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
                io_respValid<=1;
                state<=IDLE;
                io_reqReady<=1;
            end


        end
    end
    
endmodule   

