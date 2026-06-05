module InstFetchUnitExt (
    input io_clock,
    input io_reset,
    
    input [31:0] io_araddr,
    input io_arvalid,
    output reg io_arready,

    output reg [31:0] io_rdata,
    output reg [1:0] io_rresp,
    output reg io_rvalid,
    input io_rready

);

    import "DPI-C" function int mem_read(input int addr);

    parameter IDLE = 0, FETCH = 1, DELAY = 2;
    reg [1:0]state;

    always @(posedge io_clock)begin
        if(io_reset)begin
            state<=IDLE;
            io_arready<=1;
            io_rvalid<=0;
        end
        else begin
            if (state==IDLE) begin
                io_rvalid<=0;
                if(io_arvalid)begin
                    state <= FETCH;
                    io_arready <= 0;

                end
            end
            else if (state==FETCH) begin

                    io_rdata<= mem_read(io_araddr);
                    io_rvalid <= 1;
                    if(io_rready)begin
                        state <= IDLE;
                        io_arready <= 1;
                    end

            end
        end
    end
    



endmodule

