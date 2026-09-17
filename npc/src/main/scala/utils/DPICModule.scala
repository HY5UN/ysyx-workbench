package top
import chisel3._
import chisel3.util._

import chisel3._
import chisel3.util._

class ysyx_26010036_DPICModule extends ExtModule {
  val io = IO(new Bundle {
    val clk    = Input(Bool())
    val ebreak = Input(Bool())

    // difftest
    val difftest_step = Input(Bool())
    val gpr           = Input(Vec(16, UInt(32.W)))
    val csr           = Input(Vec(4, UInt(32.W)))
    val nextPC        = Input(UInt(32.W))
    val pc            = Input(UInt(32.W))
    val inst          = Input(UInt(32.W))
    val memAddr       = Input(UInt(32.W))
    val memRdata      = Input(UInt(32.W))
    val memWdata      = Input(UInt(32.W))
    val memRValid     = Input(Bool())
    val memWValid     = Input(Bool())
    val tag           = Input(UInt(8.W))
  })

  setInline(
    "ysyx_26010036_DPICModule.v",
    """
      module ysyx_26010036_DPICModule (
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
          input [31:0] io_csr_0,
          input [31:0] io_csr_1,
          input [31:0] io_csr_2,
          input [31:0] io_csr_3,
          input [31:0] io_nextPC,
          input [31:0] io_pc,
          input [31:0] io_inst,
          input [31:0] io_memAddr,
          input [31:0] io_memRdata,
          input [31:0] io_memWdata,
          input io_memRValid,
          input io_memWValid,
          input [7:0] io_tag
      );
          import "DPI-C" function void dpic_ebreak();
          import "DPI-C" function void dpic_inst_finish();
          import "DPI-C" function void dpic_save_cpu_state(
              input int nextPC,
              input int pc,
              input byte pc_tag,
              input int inst,
              input int memAddr,
              input int memRdata,
              input int memWdata,
              input bit memRValid,
              input bit memWValid,
              input int csr_0,
              input int csr_1,
              input int csr_2,
              input int csr_3
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
                  dpic_save_cpu_state(io_nextPC, io_pc,io_tag, io_inst, io_memAddr, io_memRdata, io_memWdata, io_memRValid, io_memWValid, io_csr_0, io_csr_1, io_csr_2, io_csr_3);
                  dpic_save_gprs(
                      io_gpr_0,  io_gpr_1,  io_gpr_2,  io_gpr_3,
                      io_gpr_4,  io_gpr_5,  io_gpr_6,  io_gpr_7,
                      io_gpr_8,  io_gpr_9,  io_gpr_10, io_gpr_11,
                      io_gpr_12, io_gpr_13, io_gpr_14, io_gpr_15
                  );
                  dpic_inst_finish();
              end
          end
      
      endmodule
    """.stripMargin
  )
}
