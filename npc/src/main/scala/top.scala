package top
import chisel3._
import chisel3.util._
import chisel3.probe.{force, forceInitial, read, release, releaseInitial, RWProbe, RWProbeValue}

class top extends Module {
  val io = IO(new Bundle {

    // 调试接口
    val pc     = Output(UInt(32.W))
    val inst   = Output(UInt(32.W))
    val allReg = Output(Vec(16, UInt(32.W)))
  })

  val pcReg = RegInit(0.U(32.W))
  val ifu   = Module(new InstFetchUnit())
  ifu.io.pc := pcReg

  val idu = Module(new RV32EDecoder())
  idu.io.inst := ifu.io.inst

  val reg = Module(new RegFile())

  reg.io.raddr1 := idu.io.rs1
  reg.io.raddr2 := idu.io.rs2
  reg.io.waddr  := idu.io.rd
  reg.io.wen    := idu.io.regWen

  import ControlConstants._

  val exu = Module(new ExecutionUnit())
  exu.io.op1   := Mux(idu.io.op1Sel === OP1_RS1, reg.io.rdata1, pcReg)
  exu.io.op2   := Mux(idu.io.op2Sel === OP2_RS2, reg.io.rdata2, idu.io.imm)
  exu.io.aluOp := idu.io.aluOp

  val lsu = Module(new LoadStoreUnit())
  lsu.io.addr  := exu.io.result
  lsu.io.wdata := reg.io.rdata2 << (lsu.io.addr(1, 0) * 8.U)
  lsu.io.wen   := idu.io.memWen
  lsu.io.clock := clock

  lsu.io.wmask := MuxLookup(idu.io.memLen, "b0000".U)(
    Seq(
      LEN_BYTE -> ("b0001".U << exu.io.result(1, 0)),
      LEN_HALF -> Mux(exu.io.result(1), "b1100".U, "b0011".U),
      LEN_WORD -> "b1111".U
    )
  )

  val bytes       = VecInit.tabulate(4)(i => lsu.io.rdata(8 * i + 7, 8 * i))
  val memReadData = MuxLookup(idu.io.memLen, lsu.io.rdata)(
    Seq(
      LEN_BYTE -> bytes(exu.io.result(1, 0)),
      LEN_HALF -> Mux(exu.io.result(1), Cat(bytes(3), bytes(2)), Cat(bytes(1), bytes(0))),
      LEN_WORD -> lsu.io.rdata
    )
  )

  // 写入rd
  reg.io.wdata := MuxLookup(idu.io.rdSel, exu.io.result)(
    Seq(
      RD_ALU -> exu.io.result,
      RD_MEM -> memReadData,
      RD_PC4 -> (pcReg + 4.U),
      RD_IMM -> idu.io.imm
    )
  )

  // 更新pc
  pcReg := MuxLookup(idu.io.pcSel, pcReg + 4.U)(
    Seq(
      PC_4    -> (pcReg + 4.U),
      PC_ALU  -> exu.io.result,
      PC_ALU1 -> (exu.io.result & "hfffffffe".U)
    )
  )

  // ebreak 控制
  val dpic = Module(new DPICModule())
  dpic.io.ebreak := idu.io.ebreak

  io.pc     := pcReg
  io.inst   := ifu.io.inst
  io.allReg := reg.io.regs
}
