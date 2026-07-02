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
  val exu = Module(new EXU()) // 副作用：跳转指令冲刷流水线
  val lsu = Module(new LSU()) // 副作用：内存读写
  val wbu = Module(new WBU()) // 副作用：写回GPR，CSR，异常、mret跳转冲刷流水线
  StageConnect(ifu.io.out, idu.io.in)
  StageConnect(idu.io.out, exu.io.in)
  exu.io.out <> lsu.io.in
  StageConnect(lsu.io.out, wbu.io.in)

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
  csr.io.raddr       := idu.io.out.bits.imm // idu阶段读取
  idu.io.csrRdata    := csr.io.rdata
  csr.io.waddr       := wbu.io.in.bits.imm
  csr.io.mret        := wbu.io.mret
  csr.io.wdata       := wbu.io.csrWdata
  csr.io.wen         := wbu.io.csrWen
  wbu.io.wbuCsrRdata := csr.io.wbuRdata
  csr.io.excValid    := wbu.io.excValid
  csr.io.excType     := wbu.io.excType
  csr.io.excPc       := wbu.io.in.bits.pc

  // RAW冒险处理
  val gprRAW = WireInit(false.B)
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
  when(idu.io.out.bits.ctrl.op2Sel === Op2Sel.CSR || idu.io.out.bits.ctrl.rdSel === RdSel.CSR) {
    when(
      exu.io.out.bits.ctrl.csrWen || exu.io.out.bits.ctrl.excValid ||
        lsu.io.out.bits.ctrl.csrWen || lsu.io.out.bits.ctrl.excValid ||
        wbu.io.csrWen || wbu.io.excValid
    ) {
      csrRAW := true.B
    }
  }
  exu.io.gprRAW := gprRAW
  exu.io.csrRAW := csrRAW
  // when(csrRAW || gprRAW){
  //   exu.io.in.ready :=false.B
  // }

  // 流水线冲刷处理
  ifu.io.flush := wbu.io.redirectEn || exu.io.redirectEn
  idu.io.flush := wbu.io.redirectEn || exu.io.redirectEn
  exu.io.flush := wbu.io.redirectEn
  lsu.io.flush := wbu.io.redirectEn

  ifu.io.nextPc := Mux(wbu.io.redirectEn, wbu.io.redirectPc, exu.io.redirectPc)

  // AXI4总线连接
  val arb = Module(new AXI4Arbiter())
  ifu.io.axi <> arb.io.sIFU
  lsu.io.axi <> arb.io.sLSU
  arb.io.m <> io.master

  // dpic
  val enableDpic = sys.env.getOrElse("ENABLE_DPIC", "1") == "1"
  if (enableDpic) {
    val dpic = Module(new DPICModule())
    dpic.io.ebreak := wbu.io.excValid && wbu.io.excType === ExceptionType.Breakpoint
    dpic.io.clk    := clock.asBool
    val difftest_step = RegInit(false.B)
    dpic.io.difftest_step := difftest_step
    difftest_step         := wbu.io.in.valid

    val nextPCReg = RegInit(0.U(32.W))
    val instReg   = RegInit(0.U(32.W))
    val pcReg     = RegInit(0.U(32.W))
    when(wbu.io.in.valid) {
      instReg   := wbu.io.in.bits.inst
      pcReg     := wbu.io.in.bits.pc
      nextPCReg := Mux(wbu.io.redirectEn, wbu.io.redirectPc, wbu.io.in.bits.npc)
    }

    dpic.io.nextPC := nextPCReg
    dpic.io.pc     := pcReg
    dpic.io.inst   := instReg
    dpic.io.gpr    := gpr.io.regs
    dpic.io.csr    := csr.io.dpic

    // dpic.io.pfm_begin    := ifu.io.out.bits.pc >= "h80000000".U && ifu.io.out.valid
    dpic.io.pfm_begin    := ifu.io.out.bits.pc >= "ha0000000".U && ifu.io.out.valid
    dpic.io.if_begin     := ifu.io.pfm_if_begin
    dpic.io.if_miss      := ifu.io.pfm_miss
    dpic.io.if_finish    := ifu.io.pfm_if_finish
    dpic.io.ifu_nvalid   := !ifu.io.out.valid
    dpic.io.if_bus_req   := ifu.io.axi.arvalid && ifu.io.axi.arready
    dpic.io.if_bus_resp  := ifu.io.axi.rvalid && ifu.io.axi.rready && ifu.io.axi.rlast
    dpic.io.ifu_tag       := ifu.io.out.bits.pfm_tag

    dpic.io.idu_raw     := exu.io.gprRAW || exu.io.csrRAW

    dpic.io.lsu_r_begin  := lsu.io.axi.arvalid && lsu.io.axi.arready
    dpic.io.lsu_r_finish := lsu.io.axi.rvalid && lsu.io.axi.rready && lsu.io.axi.rlast
    dpic.io.lsu_w_begin  := lsu.io.axi.awvalid && lsu.io.axi.awready
    dpic.io.lsu_w_finish := lsu.io.axi.bvalid && lsu.io.axi.bready
    dpic.io.lsu_nvalid   := !lsu.io.out.valid && !lsu.io.in.ready

    dpic.io.wbu_valid    := wbu.io.in.valid
    dpic.io.wbu_tag      := wbu.io.in.bits.pfm_tag

    dpic.io.inst_r       := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.R && wbu.io.in.valid
    dpic.io.inst_i       := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.I && wbu.io.in.valid
    dpic.io.inst_l       := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.L && wbu.io.in.valid
    dpic.io.inst_s       := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.S && wbu.io.in.valid
    dpic.io.inst_b       := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.B && wbu.io.in.valid
    dpic.io.inst_u       := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.U && wbu.io.in.valid
    dpic.io.inst_j       := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.J && wbu.io.in.valid
    dpic.io.inst_csr     := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.CSR && wbu.io.in.valid
    dpic.io.inst_sys     := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.SYS && wbu.io.in.valid
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
      right.bits := RegEnable(left.bits, right.ready)
      // val rightValid = RegInit(false.B)
      // rightValid  := left.valid && left.fire
      // right.valid := rightValid
      val validReg = RegEnable(left.valid, right.ready)
      when(right.ready){
        right.valid := validReg
      }.otherwise{
        right.valid := false.B
      }
    }
    // else if (arch == "ooo") { right <> Queue(left, 16) }
  }
}
