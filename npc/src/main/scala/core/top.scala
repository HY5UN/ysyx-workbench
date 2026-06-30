package top
import chisel3._
import chisel3.util._
import chisel3.probe.{force, forceInitial, read, release, releaseInitial, RWProbe, RWProbeValue}

class ysyx_26010036 extends Module {
  val io = IO(new Bundle {
    val interrupt = Input(Bool())
    val master    = new AXI4IO
    val slave     = Flipped(new AXI4IO)
  })
  ChiselUtils.driveZeroOutputs(io)

  val ifu = Module(new IFU())
  val idu = Module(new IDU())
  val exu = Module(new EXU())
  val lsu = Module(new LSU())
  val wbu = Module(new WBU())
  StageConnect(ifu.io.out, idu.io.in)
  StageConnect(idu.io.out, exu.io.in)
  exu.io.out <> lsu.io.in
  StageConnect(lsu.io.out, wbu.io.in)
  wbu.io.out <> ifu.io.in

  val gpr = Module(new RegFile())

  // gpr
  gpr.io.raddr1 := idu.io.rs1 // idu阶段读取
  gpr.io.raddr2 := idu.io.rs2
  idu.io.rdata1 := gpr.io.rdata1
  idu.io.rdata2 := gpr.io.rdata2
  gpr.io.wen    := wbu.io.wen // wbu阶段写回
  gpr.io.waddr  := wbu.io.rd
  gpr.io.wdata  := wbu.io.wdata

  val csr = Module(new CSRFile())

  // CSR
  csr.io.raddr       := idu.io.out.bits.imm // idu阶段解码与读取
  idu.io.csrRdata    := csr.io.rdata
  csr.io.waddr       := wbu.io.in.bits.imm
  csr.io.ecall       := wbu.io.ecall        // wbu阶段写回
  csr.io.mret        := wbu.io.mret
  csr.io.wdata       := wbu.io.csrWdata
  csr.io.wen         := wbu.io.csrWen
  wbu.io.wbuCsrRdata := csr.io.wbuRdata

  val gprRAW = WireInit(false.B)
  idu.io.gprRAW := gprRAW
  when(idu.io.rs1 =/= 0.U) {
    when(idu.io.out.bits.ctrl.op1Sel === Op1Sel.RS1 || idu.io.out.bits.ctrl.csrSel === CsrSel.RS1) {

      when(
        (exu.io.out.bits.rd === idu.io.rs1 && exu.io.out.bits.ctrl.regWen) ||
          (lsu.io.out.bits.rd === idu.io.rs1 && lsu.io.out.bits.ctrl.regWen) ||
          (wbu.io.rd === idu.io.rs1 && wbu.io.wen)
      ) {

        gprRAW := true.B
      }
    }
  }
  when(idu.io.rs2 =/= 0.U) {
    when(idu.io.out.bits.ctrl.op2Sel === Op2Sel.RS2) {
      when(
        (exu.io.out.bits.rd === idu.io.rs2 && exu.io.out.bits.ctrl.regWen) ||
          (lsu.io.out.bits.rd === idu.io.rs2 && lsu.io.out.bits.ctrl.regWen) ||
          (wbu.io.rd === idu.io.rs2 && wbu.io.wen)
      ) {
        gprRAW := true.B
      }
    }
  }

  val csrRAW = WireInit(false.B)
  idu.io.csrRAW := csrRAW
  when(idu.io.out.bits.ctrl.op2Sel === Op2Sel.CSR || idu.io.out.bits.ctrl.rdSel === RdSel.CSR) {
    when(
      exu.io.out.bits.ctrl.csrWen || exu.io.out.bits.ctrl.ecall ||
        lsu.io.out.bits.ctrl.csrWen || lsu.io.out.bits.ctrl.ecall ||
        wbu.io.csrWen || wbu.io.ecall
    ) {
      csrRAW := true.B
    }
  }

  // AXI4总线连接
  val arb = Module(new AXI4Arbiter())
  ifu.io.axi <> arb.io.sIFU
  lsu.io.axi <> arb.io.sLSU
  arb.io.m <> io.master

  // dpic
  val enableDpic = sys.env.getOrElse("ENABLE_DPIC", "1") == "1"

  if (enableDpic) {
    val dpic = Module(new DPICModule())
    dpic.io.ebreak := idu.io.out.bits.ctrl.ebreak
    dpic.io.clk    := clock.asBool
    val difftest_step = RegInit(false.B)
    dpic.io.difftest_step := difftest_step
    difftest_step         := ifu.io.in.fire
    val nextPCReg = RegInit(0.U(32.W))
    val instReg   = RegInit(0.U(32.W))
    val pcReg     = RegInit(0.U(32.W))
    when(ifu.io.in.fire) {
      instReg   := ifu.io.out.bits.inst
      pcReg     := ifu.io.out.bits.pc
      nextPCReg := wbu.io.out.bits.nextPC
    }
    dpic.io.nextPC := nextPCReg
    dpic.io.pc   := pcReg
    dpic.io.inst := instReg
    dpic.io.gpr  := gpr.io.regs
    dpic.io.csr  := csr.io.dpic

    dpic.io.if_begin     := ifu.io.in.fire
    dpic.io.if_miss      := ifu.io.miss
    dpic.io.if_finish    := ifu.io.out.valid
    dpic.io.lsu_r_begin  := lsu.io.axi.arvalid && lsu.io.axi.arready
    dpic.io.lsu_r_finish := lsu.io.axi.rvalid && lsu.io.axi.rready
    dpic.io.lsu_w_begin  := lsu.io.axi.awvalid && lsu.io.axi.awready
    dpic.io.lsu_w_finish := lsu.io.axi.bvalid && lsu.io.axi.bready
    dpic.io.exu          := exu.io.out.fire
    dpic.io.inst_r       := idu.io.out.bits.ctrl.pcit === PfmCntInstType.R && idu.io.out.fire
    dpic.io.inst_i       := idu.io.out.bits.ctrl.pcit === PfmCntInstType.I && idu.io.out.fire
    dpic.io.inst_l       := idu.io.out.bits.ctrl.pcit === PfmCntInstType.L && idu.io.out.fire
    dpic.io.inst_s       := idu.io.out.bits.ctrl.pcit === PfmCntInstType.S && idu.io.out.fire
    dpic.io.inst_b       := idu.io.out.bits.ctrl.pcit === PfmCntInstType.B && idu.io.out.fire
    dpic.io.inst_u       := idu.io.out.bits.ctrl.pcit === PfmCntInstType.U && idu.io.out.fire
    dpic.io.inst_j       := idu.io.out.bits.ctrl.pcit === PfmCntInstType.J && idu.io.out.fire
    dpic.io.inst_csr     := idu.io.out.bits.ctrl.pcit === PfmCntInstType.CSR && idu.io.out.fire
    dpic.io.inst_sys     := idu.io.out.bits.ctrl.pcit === PfmCntInstType.SYS && idu.io.out.fire
  }
}

object StageConnect {
  def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) = {
    val arch = "pipeline"
    if (arch == "single") {
      right := left
    } else if (arch == "multi") { right <> left }
    else if (arch == "pipeline") {
      left.ready := right.ready
      right.bits := RegEnable(left.bits, left.fire)
      val rightValid = RegInit(false.B)
      rightValid  := left.valid
      right.valid := rightValid
    }
    // else if (arch == "ooo") { right <> Queue(left, 16) }
  }
}
