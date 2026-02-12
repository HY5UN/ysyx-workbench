module DPICModule (
    input ebreak,
);
    import "DPI-C" function void ebreak();

    always @(*) begin
        if (ebreak) begin
            ebreak();
        end
    end
    
endmodule