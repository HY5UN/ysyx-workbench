package top
import chisel3._
import chisel3.util._

class IFU2ICA extends Bundle {
  val pc       = UInt(32.W)
  val pc4      = UInt(32.W)
  val branchPreTaken = Bool()
  val dpic_tag = UInt(8.W)

}

class BTBEntry extends Bundle {
  val tag    = UInt()
  val target = UInt()
}

class IFU extends Module {
  val io          = IO(new Bundle {
    val out        = Decoupled(new IFU2ICA)
    val redirectEn = Input(Bool())
    val redirectPc = Input(UInt(32.W))
    val pcOfBranch = Input(UInt(32.W))

  })
  val pc          = RegInit("h30000000".U(32.W))
  val pc4         = WireInit((pc + 4.U)(31, 0))
  val dpic_tagReg = RegInit(0.U(8.W))
  io.out.bits.pc       := pc
  io.out.bits.pc4      := pc4
  io.out.bits.branchPreTaken := false.B
  io.out.bits.dpic_tag := dpic_tagReg
  io.out.valid         := false.B

  // BTB参数计算
  val numEntries = 4
  val assoc      = 4
  val numGroups  = numEntries / assoc
  val accessPc   = WireInit(pc)
  val indexLen   = log2Ceil(numGroups)
  val index      = if (indexLen > 0) accessPc(indexLen + 1, 2) else 0.U
  val tag        = accessPc(31, indexLen + 2)

  val btb      = Reg(Vec(numGroups, Vec(assoc, new BTBEntry)))
  val validArr = RegInit(VecInit(Seq.fill(numGroups)(VecInit(Seq.fill(assoc)(false.B)))))

  val wayHitsOH = VecInit((0 until assoc).map(i => btb(index)(i).tag === tag && validArr(index)(i)))
  val wayDatas  = VecInit((0 until assoc).map(i => btb(index)(i).target))
  val hit       = wayHitsOH.asUInt.orR
  val target    = Mux1H(wayHitsOH, wayDatas)

  val plruBits   =
    if (assoc > 1) Some(RegInit(VecInit(Seq.fill(numGroups)(VecInit(Seq.fill(assoc - 1)(false.B)))))) else None
  val wayHitIdx  = OHToUInt(wayHitsOH)
  val replaceWay = if (assoc > 1) PLRU.victim(plruBits.get(index)) else 0.U

  when(io.redirectEn) {
    pc          := io.redirectPc
    dpic_tagReg := dpic_tagReg + 1.U

    accessPc    := io.pcOfBranch
    when(!hit) {
      validArr(index)(replaceWay)   := true.B
      btb(index)(replaceWay).tag    := tag
      btb(index)(replaceWay).target := io.redirectPc
      if (assoc > 1) PLRU.access(plruBits.get(index), replaceWay)
    }
  }.otherwise {
    io.out.valid := true.B
    accessPc := pc
    when(io.out.ready) {
      pc          := pc4
      dpic_tagReg := dpic_tagReg + 1.U
      when(hit) {
        pc := target // always taken
        io.out.bits.branchPreTaken:= true.B
        if(assoc>1) PLRU.access(plruBits.get(index),wayHitIdx)

      }
    }
  }

}
