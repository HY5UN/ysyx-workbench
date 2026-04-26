
module MemExt (
    input io_clock,
    input io_reqValid,
    output reg io_respValid,
    input [31:0] io_addr,
    input [31:0] io_wdata,
    output reg [31:0] io_rdata,
    input io_wen,
    input [3:0] io_wmask
);
    import "DPI-C" function int  mem_read(input int addr);
    import "DPI-C" function void mem_write(input int addr, input int data, input byte wmask);

    reg [1:0] delayCounter;

    parameter IDLE = 0, WAIT = 1;
    reg state;
    always @(posedge io_clock) begin
        if(state == IDLE) begin
            io_respValid <= 0;
            if(io_reqValid) begin
                state <= WAIT;
            end
        end
        else if(state == WAIT) begin
            if(delayCounter == 0) begin
                state <= IDLE;
                io_respValid <= 1;
            end
        end
    end

    always @(posedge io_clock) begin
        if(io_reqValid) begin
            io_rdata<= !io_wen ? mem_read(io_addr) : 32'h0;
            if(io_wen) begin
                mem_write(io_addr, io_wdata, {4'b0, io_wmask});
            end 

            

        end
        delayCounter <= delayCounter + 1;
        
        
    end
    
endmodule   

