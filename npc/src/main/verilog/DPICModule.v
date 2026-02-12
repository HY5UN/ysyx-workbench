module DPICModule (
    input io_ebreak,
);
    import "DPI-C" function void ebreak();

    always @(*) begin
        if (io_ebreak) begin
            ebreak();
        end
    end
    
endmodule