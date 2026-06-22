module DPICModule (
    input        io_clk,
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
    input [31:0] io_inst,
    input io_instfetch,
    input io_lsu_r,
    input io_lsu_w,
    input io_exu,
    input io_inst_r,
    input io_inst_i,
    input io_inst_ls,
    input io_inst_u,
    input io_inst_b,
    input io_inst_j,
    input io_inst_csr,
    input io_inst_sys

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
    import "DPI-C" function void dpic_save_performance_event(
        input bit instfetch,
        input bit lsu_r,
        input bit lsu_w,
        input bit exu,
        input bit inst_r,
        input bit inst_i,
        input bit inst_ls,
        input bit inst_u,
        input bit inst_b,
        input bit inst_j,
        input bit inst_csr,
        input bit inst_sys
    );

    always @(*) begin
        if (io_ebreak) begin
            dpic_ebreak();
        end
        if (io_difftest_step) begin
            dpic_save_cpu_state(io_nextPC, io_pc, io_inst);
            dpic_save_gprs(
                io_gpr_0,  io_gpr_1,  io_gpr_2,  io_gpr_3,
                io_gpr_4,  io_gpr_5,  io_gpr_6,  io_gpr_7,
                io_gpr_8,  io_gpr_9,  io_gpr_10, io_gpr_11,
                io_gpr_12, io_gpr_13, io_gpr_14, io_gpr_15
            );
            dpic_inst_finish();
        end
    end

    always @(posedge io_clk) begin
        dpic_save_performance_event(
            io_instfetch,
            io_lsu_r,
            io_lsu_w,
            io_exu,
            io_inst_r,
            io_inst_i,
            io_inst_ls,
            io_inst_u,
            io_inst_b,
            io_inst_j,
            io_inst_csr,
            io_inst_sys
        );
    end

endmodule