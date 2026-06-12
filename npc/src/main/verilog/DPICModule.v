module DPICModule (
    input        io_ebreak,
    input        io_difftest_step,
    input [31:0] io_gpr_0,
    input [31:0] io_gpr_1,
    input [31:0] io_gpr_2,
    input [31:0] io_gpr_3,
    input [31:0] io_gpr_4,
    input [31:0] io_gpr_5,
    input [31:0] io_gpr_6,
    input [31:0] io_gpr_7,
    input [31:0] io_gpr_8,
    input [31:0] io_gpr_9,
    input [31:0] io_gpr_10,
    input [31:0] io_gpr_11,
    input [31:0] io_gpr_12,
    input [31:0] io_gpr_13,
    input [31:0] io_gpr_14,
    input [31:0] io_gpr_15,
    input [31:0] io_nextPC,
    input [31:0] io_pc,
    input [31:0] io_inst
);
    import "DPI-C" function void dpic_ebreak();
    import "DPI-C" function void dpic_inst_finish();
    import "DPI-C" function void dpic_save_cpu_state(
        input int nextPC,
        input int pc,
        input int inst
    );
    import "DPI-C" function void dpic_save_gprs(
        input int gpr0,  input int gpr1,  input int gpr2,  input int gpr3,
        input int gpr4,  input int gpr5,  input int gpr6,  input int gpr7,
        input int gpr8,  input int gpr9,  input int gpr10, input int gpr11,
        input int gpr12, input int gpr13, input int gpr14, input int gpr15
    );

    always @(*) begin
        if (io_ebreak) begin
            dpic_ebreak();
        end
        if (io_difftest_step) begin
            dpic_get_pc(io_nextPC, io_pc);
            dpic_get_gprs(
                io_gpr_0,  io_gpr_1,  io_gpr_2,  io_gpr_3,
                io_gpr_4,  io_gpr_5,  io_gpr_6,  io_gpr_7,
                io_gpr_8,  io_gpr_9,  io_gpr_10, io_gpr_11,
                io_gpr_12, io_gpr_13, io_gpr_14, io_gpr_15
            );
            dpic_inst_finish();
        end
    end

endmodule