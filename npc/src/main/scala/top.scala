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
  val ica = Module(new ICache(cacheSizeB = 128, blockSizeB = 16, assoc = 2))
  val idu = Module(new IDU())
  val exu = Module(new EXU()) // 副作用：跳转指令冲刷流水线
  val lsu = Module(new LSU()) // 副作用：内存读写
  val wbu = Module(new WBU()) // 副作用：写回GPR，CSR，异常、mret跳转冲刷流水线

  val exuFlush, wbuFlush = WireInit(false.B)
  StageConnect(ifu.io.out, ica.io.in, exuFlush)
  StageConnect(ica.io.out, idu.io.in, exuFlush)
  StageConnect(idu.io.out, exu.io.in, wbuFlush)
  StageConnect(exu.io.out, lsu.io.in, wbuFlush)
  StageConnect(lsu.io.out, wbu.io.in, false.B)

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
  val rs1RAW      = WireInit(false.B)
  val rs1fwdValid = WireInit(false.B)
  idu.io.raw.rs1RAW      := rs1RAW
  idu.io.raw.rs1fwdValid := rs1fwdValid

  when(idu.io.raw.rs1R) {
    when(exu.io.out.valid && exu.io.out.bits.rd === idu.io.rs1 && exu.io.out.bits.ctrl.regWen) {
      rs1RAW := true.B

      idu.io.rdata1 := exu.io.out.bits.gprWdata
      rs1fwdValid   := exu.io.out.bits.ctrl.rdSel =/= RdSel.MEM

    }.elsewhen(lsu.io.out.valid && lsu.io.out.bits.rd === idu.io.rs1 && lsu.io.out.bits.ctrl.regWen) {
      rs1RAW        := true.B
      idu.io.rdata1 := lsu.io.out.bits.gprWdata
      rs1fwdValid   := true.B
    }.elsewhen(wbu.io.rd === idu.io.rs1 && wbu.io.wen) {
      rs1RAW        := true.B
      idu.io.rdata1 := wbu.io.wdata
      rs1fwdValid   := true.B
    }
  }
  val rs2RAW      = WireInit(false.B)
  val rs2fwdValid = WireInit(false.B)
  idu.io.raw.rs2RAW      := rs2RAW
  idu.io.raw.rs2fwdValid := rs2fwdValid

  when(idu.io.raw.rs2R) {
    when(exu.io.out.valid && exu.io.out.bits.rd === idu.io.rs2 && exu.io.out.bits.ctrl.regWen) {
      rs2RAW        := true.B
      idu.io.rdata2 := exu.io.out.bits.gprWdata
      rs2fwdValid   := exu.io.out.bits.ctrl.rdSel =/= RdSel.MEM
    }.elsewhen(lsu.io.out.valid && lsu.io.out.bits.rd === idu.io.rs2 && lsu.io.out.bits.ctrl.regWen) {
      rs2RAW        := true.B
      idu.io.rdata2 := lsu.io.out.bits.gprWdata
      rs2fwdValid   := true.B
    }.elsewhen(wbu.io.rd === idu.io.rs2 && wbu.io.wen) {
      rs2RAW        := true.B
      idu.io.rdata2 := wbu.io.wdata
      rs2fwdValid   := true.B
    }
  }

  val csrRAW = WireInit(false.B)
  idu.io.raw.csrRAW := csrRAW

  when(idu.io.out.bits.ctrl.op2Sel === Op2Sel.CSR || idu.io.out.bits.ctrl.rdSel === RdSel.CSR) {
    when(
      (exu.io.out.valid && (exu.io.out.bits.ctrl.csrWen || exu.io.out.bits.ctrl.excValid)) ||
        (lsu.io.out.valid && (lsu.io.out.bits.ctrl.csrWen || lsu.io.out.bits.ctrl.excValid)) ||
        wbu.io.csrWen || wbu.io.excValid
    ) {
      csrRAW := true.B
    }
  }

  // 流水线冲刷处理
  exuFlush          := wbu.io.redirectEn || exu.io.redirectEn
  wbuFlush          := wbu.io.redirectEn
  ifu.io.redirectEn := exuFlush
  ifu.io.redirectPc := Mux(wbu.io.redirectEn, wbu.io.redirectPc, exu.io.redirectPc)
  ifu.io.pcOfBranch := exu.io.pcOfBranch

  // AXI4总线连接
  val arb = Module(new AXI4Arbiter())
  ica.io.axi <> arb.io.sIFU
  lsu.io.axi <> arb.io.sLSU
  arb.io.m <> io.master

  // dpic
  val enableDpic = sys.env.getOrElse("ENABLE_DPIC", "1") == "1"
  if (enableDpic) {
    val dpic = Module(new DPICModule())
    dpic.io.ebreak := wbu.io.excValid && wbu.io.excType === ExceptionType.Breakpoint
    dpic.io.clk    := clock.asBool

    // difftest
    dpic.io.difftest_step := RegNext(wbu.io.in.valid)
    dpic.io.nextPC        := RegEnable(Mux(wbu.io.redirectEn, wbu.io.redirectPc, wbu.io.in.bits.dpic_npc), wbu.io.in.valid)
    dpic.io.pc            := RegEnable(wbu.io.dpic_pc, wbu.io.in.valid)
    dpic.io.inst          := RegEnable(wbu.io.dpic_inst, wbu.io.in.valid)
    dpic.io.gpr           := gpr.io.regs
    dpic.io.csr           := csr.io.dpic
    dpic.io.memAddr       := RegEnable(wbu.io.dpic_memAddr, wbu.io.in.valid)
    dpic.io.memRdata      := RegEnable(wbu.io.dpic_memRdata, wbu.io.in.valid)
    dpic.io.memWdata      := RegEnable(wbu.io.dpic_memWdata, wbu.io.in.valid)
    dpic.io.memRValid     := RegEnable(wbu.io.dpic_memRValid, wbu.io.in.valid)
    dpic.io.memWValid     := RegEnable(wbu.io.dpic_memWValid, wbu.io.in.valid)

    // performance counter
    // dpic.io.pfm_begin    := ifu.io.out.bits.pc >= "h80000000".U && ifu.io.out.valid
    dpic.io.pfm_begin     := ifu.io.out.bits.pc >= "ha0000000".U && ifu.io.out.valid
    dpic.io.if_miss       := false.B // todo
    dpic.io.if_finish     := ifu.io.out.fire
    dpic.io.ifu_i_flushed := false.B // todo
    dpic.io.ifu_nvalid    := !ifu.io.out.valid
    dpic.io.if_bus_req    := ica.io.axi.arvalid && ica.io.axi.arready
    dpic.io.if_bus_resp   := ica.io.axi.rvalid && ica.io.axi.rready && ica.io.axi.rlast
    dpic.io.ifu_tag       := ifu.io.out.bits.dpic_tag

    dpic.io.idu_raw := (idu.io.raw.rs1RAW && !idu.io.raw.rs1fwdValid) || (idu.io.raw.rs2RAW && !idu.io.raw.rs2fwdValid) || idu.io.raw.csrRAW

    dpic.io.lsu_r_begin  := lsu.io.axi.arvalid && lsu.io.axi.arready
    dpic.io.lsu_r_finish := lsu.io.axi.rvalid && lsu.io.axi.rready && lsu.io.axi.rlast
    dpic.io.lsu_w_begin  := lsu.io.axi.awvalid && lsu.io.axi.awready
    dpic.io.lsu_w_finish := lsu.io.axi.bvalid && lsu.io.axi.bready
    dpic.io.lsu_nvalid   := !lsu.io.in.ready

    dpic.io.wbu_valid := wbu.io.in.valid
    dpic.io.wbu_tag   := wbu.io.dpic_tag

    dpic.io.inst_r   := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.R && wbu.io.in.valid
    dpic.io.inst_i   := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.I && wbu.io.in.valid
    dpic.io.inst_l   := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.L && wbu.io.in.valid
    dpic.io.inst_s   := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.S && wbu.io.in.valid
    dpic.io.inst_b   := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.B && wbu.io.in.valid
    dpic.io.inst_u   := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.U && wbu.io.in.valid
    dpic.io.inst_j   := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.J && wbu.io.in.valid
    dpic.io.inst_csr := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.CSR && wbu.io.in.valid
    dpic.io.inst_sys := wbu.io.in.bits.ctrl.pcit === PfmCntInstType.SYS && wbu.io.in.valid
  }
}

object StageConnect {
  def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T], flush: Bool = false.B) = {
    val arch = "pipeline"
    if (arch == "single") { right := left }
    else if (arch == "multi") { right <> left }
    else if (arch == "pipeline") {
      left.ready := right.ready
      right.bits := RegEnable(left.bits, right.ready)
      val validReg = RegInit(false.B)
      right.valid := validReg

      when(flush) {
        validReg    := false.B
        right.valid := false.B
      }.elsewhen(right.ready) {
        validReg := left.valid
      }
    }
    // else if (arch == "ooo") { right <> Queue(left, 16) }
  }
}
