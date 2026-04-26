module DPICModule (
    input io_ebreak,
    input io_difftest_step
);
    import "DPI-C" function void dpic_ebreak();
    import "DPI-C" function void dpic_difftest_step();

    always @(*) begin
        if (io_ebreak) begin
            dpic_ebreak();
        end
        if (io_difftest_step) begin
            dpic_difftest_step();
        end
    end
    
endmodule