package top
import chisel3._
import chisel3.util._
import chisel3.probe.{force, forceInitial, read, release, releaseInitial, RWProbe, RWProbeValue}


class ysyx_26010036 extends Module {
  val io = IO(new Bundle {
    val interrupt = Input(Bool())
    val master   = new AXI4IO
    val slave   = Flipped(new AXI4IO)
  })
  val mtie0 = Module(new AXI4MasterTie0())
  val stie0 = Module(new AXI4SlaveTie0())
  mtie0.io.m <> io.master
  io.slave <> stie0.io.s

  val ifu = Module(new InstFetchUnit())
  val idu = Module(new RV32EDecoder())
  val exu = Module(new ExecutionUnit())
  val lsu = Module(new LoadStoreUnit())
  val wbu = Module(new WriteBackUnit())
  StageConnect(ifu.io.out, idu.io.in)
  StageConnect(idu.io.out, exu.io.in)
  StageConnect(exu.io.out, lsu.io.in)
  StageConnect(lsu.io.out, wbu.io.in)
  StageConnect(wbu.io.out, ifu.io.in)

  val reg = Module(new RegFile())

  // Reg
  reg.io.raddr1 := idu.io.rs1 // idu阶段读取
  reg.io.raddr2 := idu.io.rs2
  exu.io.rdata1 := reg.io.rdata1
  exu.io.rdata2 := reg.io.rdata2
  reg.io.wen    := wbu.io.wen // wbu阶段写回
  reg.io.waddr  := wbu.io.rd
  reg.io.wdata  := wbu.io.wdata

  val csr = Module(new CSRFile())

  // CSR
  csr.io.addr     := idu.io.out.bits.imm// idu阶段解码与读取
  exu.io.csrRdata := csr.io.rdata
  csr.io.ecall    := wbu.io.ecall // wbu阶段写回
  csr.io.mret     := wbu.io.mret
  csr.io.wdata    := wbu.io.csrWdata            
  csr.io.wen      := wbu.io.csrWen
  wbu.io.csrRdata := csr.io.rdata 

  // AXI4总线连接
  val arb = Module(new AXI4Arbiter())
  ifu.io.axi <> arb.io.sIFU
  lsu.io.axi <> arb.io.sLSU
  arb.io.m <> io.master
  


  // dpic 控制
  val dpic = Module(new DPICModule())
  dpic.io.clk := clock.asBool
  dpic.io.ebreak := idu.io.out.bits.ctrl.ebreak
  val difftest_step = RegInit(false.B) // 延迟一拍等待寄存器更新
  dpic.io.difftest_step := difftest_step
  difftest_step         := ifu.io.in.fire
  val nextPCReg = RegInit(0.U(32.W))
  val instReg = RegInit(0.U(32.W))
  val pcReg = RegInit(0.U(32.W))
  when(ifu.io.in.fire) {
    instReg := ifu.io.out.bits.inst
    pcReg := ifu.io.out.bits.pc
    nextPCReg := wbu.io.out.bits.nextPC
  }
  dpic.io.nextPC := nextPCReg
  dpic.io.pc := pcReg
  dpic.io.inst := instReg
  dpic.io.gpr := reg.io.regs

  dpic.io.if_begin := ifu.io.axi.arvalid && ifu.io.axi.arready
  dpic.io.if_finish := ifu.io.axi.rvalid && ifu.io.axi.rready
  dpic.io.lsu_r_begin := lsu.io.axi.arvalid && lsu.io.axi.arready
  dpic.io.lsu_r_finish := lsu.io.axi.rvalid && lsu.io.axi.rready
  dpic.io.lsu_w_begin := lsu.io.axi.awvalid && lsu.io.axi.awready
  dpic.io.lsu_w_finish := lsu.io.axi.wvalid && lsu.io.axi.wready
  dpic.io.exu := exu.io.out.fire
  dpic.io.inst_r := idu.io.out.bits.ctrl.pcit === PfmCntInstType.R && idu.io.out.fire
  dpic.io.inst_i := idu.io.out.bits.ctrl.pcit === PfmCntInstType.I && idu.io.out.fire
  dpic.io.inst_l := idu.io.out.bits.ctrl.pcit === PfmCntInstType.L && idu.io.out.fire
  dpic.io.inst_s := idu.io.out.bits.ctrl.pcit === PfmCntInstType.S && idu.io.out.fire
  dpic.io.inst_b := idu.io.out.bits.ctrl.pcit === PfmCntInstType.B && idu.io.out.fire
  dpic.io.inst_u := idu.io.out.bits.ctrl.pcit === PfmCntInstType.U && idu.io.out.fire
  dpic.io.inst_j := idu.io.out.bits.ctrl.pcit === PfmCntInstType.J && idu.io.out.fire
  dpic.io.inst_csr := idu.io.out.bits.ctrl.pcit === PfmCntInstType.CSR && idu.io.out.fire
  dpic.io.inst_sys := idu.io.out.bits.ctrl.pcit === PfmCntInstType.SYS && idu.io.out.fire

}

object StageConnect {
  def apply[T <: Data](left: DecoupledIO[T], right: DecoupledIO[T]) = {
    val arch = "multi"
    // 为展示抽象的思想, 此处代码省略了若干细节
    if (arch == "single") {
      right <> left
    } else if (arch == "multi") { right <> left }
    // else if (arch == "pipeline") { right <> RegEnable(left, left.fire) }
    // else if (arch == "ooo") { right <> Queue(left, 16) }
  }
}
