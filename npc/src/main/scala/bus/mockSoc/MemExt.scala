package top
import chisel3._
import chisel3.util._

class MemExt extends Module {
  val io = IO(new Bundle {
    val axi = Flipped(new AXI4IO)
  })
  ChiselUtils.driveZeroOutputs(io.axi)

  val mem = Module(new MemHelper())

  object State extends ChiselEnum {
    val sIdle, sWait, sDone = Value
  }
  val waddrReg = RegInit(0.U(32.W))
  val wdataReg = RegInit(0.U(32.W))
  val wmaskReg = RegInit(0.U(4.W))

  val wstate = RegInit(State.sIdle)
  val bstate = RegInit(State.sIdle)

  switch(wstate) {
    is(State.sIdle) {
      io.axi.wready := true.B
      when(io.axi.wvalid) {
        wstate   := State.sDone
        wdataReg := io.axi.wdata
        wmaskReg := io.axi.wstrb
      }
    }
    is(State.sDone) {
      io.axi.wready := false.B
      when(io.axi.bready && io.axi.bvalid) {
        wstate := State.sIdle
      }
    }
  }

  switch(bstate) {
    is(State.sIdle) {
      io.axi.awready := true.B
      when(io.axi.awvalid) {
        bstate   := State.sWait
        waddrReg := io.axi.awaddr
      }
    }
    is(State.sWait) {
      when(wstate === State.sDone) {
        mem.io.wen := true.B

      }
    }
    is(State.sDone) {
      io.axi.bvalid := true.B
      when(io.axi.bready) {
        bstate := State.sIdle
      }
    }
  }

  object Rstate extends ChiselEnum {
    val sIdle, sRead, sOut = Value
  }
  val rstate = RegInit(Rstate.sIdle)
  val raddrReg   = RegInit(0.U(32.W))
  val arburstReg = RegInit(0.U(2.W))
  val arlenReg   = RegInit(0.U(8.W))
  val rdataReg   = RegInit(0.U(32.W))
  val burstCnt   = RegInit(0.U(8.W))
  switch(rstate) {
    is(Rstate.sIdle) {
      io.axi.arready := true.B
      when(io.axi.arvalid) {
        rstate     := Rstate.sRead
        raddrReg   := io.axi.araddr
        arburstReg := io.axi.arburst
        arlenReg   := io.axi.arlen
      }
    }
    is(Rstate.sRead) {
      mem.io.ren := true.B
      rstate     := Rstate.sOut
      rdataReg   := mem.io.rdata
      raddrReg   := raddrReg + 4.U
      burstCnt   := burstCnt + 1.U
    }
    is(Rstate.sOut) {
      io.axi.rvalid := true.B
      io.axi.rdata  := rdataReg
      when(io.axi.rready) {
        when(burstCnt === arlenReg + 1.U) {
          rstate := Rstate.sIdle
        }.otherwise {
          rstate := Rstate.sRead
        }
      }
    }
  }

  mem.io.clock := clock
  mem.io.reset := reset
  mem.io.wen   := false.B
  mem.io.ren   := false.B
  mem.io.waddr := waddrReg
  mem.io.wdata := wdataReg
  mem.io.wstrb := wmaskReg
  mem.io.raddr := raddrReg
}

class MemHelper extends ExtModule {
  val io = IO(new Bundle {
    val clock = Input(Clock())
    val reset = Input(Bool())
    val raddr = Input(UInt(32.W))
    val waddr = Input(UInt(32.W))
    val ren   = Input(Bool())
    val rdata = Output(UInt(32.W))
    val wdata = Input(UInt(32.W))
    val wen   = Input(Bool())
    val wstrb = Input(UInt(4.W))
  })

  setInline(
    "MemHelper.v",
    """
    module MemHelper(
      input         io_clock,
      input         io_reset,
      input  [31:0] io_raddr,
      input  [31:0] io_waddr,
      input         io_ren,
      output reg [31:0] io_rdata,
      input  [31:0] io_wdata,
      input         io_wen,
      input  [3:0]  io_wstrb
    );
      import "DPI-C" function int mem_read(input int addr);
      import "DPI-C" function void mem_write(input int addr, input int data, input byte wmask);

      always @(posedge io_clock) begin
        if(io_wen)
          mem_write(io_waddr, io_wdata, io_wstrb);
      end

      always @(*) begin
        if(io_ren)
          io_rdata = mem_read(io_raddr);
        else
          io_rdata = 32'h0;
      end
    endmodule
    """.stripMargin
  )
}
