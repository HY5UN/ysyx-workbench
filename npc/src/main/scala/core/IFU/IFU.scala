package top
import chisel3._
import chisel3.util._

class IFU2ICA extends Bundle {
  val pc             = UInt(32.W)
  val pc4            = UInt(32.W)
  val branchPreTaken = Bool()
  val dpic_tag       = UInt(8.W)

}

class BTBEntry extends Bundle {
  val tag    = UInt()
  val target = UInt()
}

class IFU extends Module {
  val io            = IO(new Bundle {
    val out        = Decoupled(new IFU2ICA)
    val redirectEn = Input(Bool())
    val redirectPc = Input(UInt(32.W))
    val pcOfBranch = Input(UInt(32.W))
    

  })
  val pc            = RegInit("h30000000".U(32.W))
  // val pc          = RegInit("h80000000".U(32.W))
  val pc4           = WireInit((pc + 4.U)(31, 0))
  val dpic_tagReg   = RegInit(0.U(8.W))
  val pcOfBranchReg = RegEnable(io.pcOfBranch, io.redirectEn)
  io.out.bits.pc       := pc
  io.out.bits.pc4      := pc4
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

  val updateBTB = RegInit(false.B)

  val branchTaken  = WireInit(false.B)
  val branchNextPc = WireInit(target)
  io.out.bits.branchPreTaken := branchTaken

  when(io.redirectEn) {
    pc          := io.redirectPc
    dpic_tagReg := dpic_tagReg + 1.U
    updateBTB   := true.B

  }.otherwise {
    io.out.valid := true.B
    when(io.out.ready) {
      pc          := Mux(branchTaken, branchNextPc, pc4)
      dpic_tagReg := dpic_tagReg + 1.U

    }
  }

  when(updateBTB) {
    accessPc  := pcOfBranchReg
    when(!hit) {
      validArr(index)(replaceWay)   := true.B
      btb(index)(replaceWay).tag    := tag
      btb(index)(replaceWay).target := pc
      if (assoc > 1) PLRU.access(plruBits.get(index), replaceWay)
    }
    updateBTB := false.B
  }.otherwise {
    accessPc := pc
    // when(hit) {
    //   branchTaken:= true.B //always taken
    //   if(assoc>1) PLRU.access(plruBits.get(index),wayHitIdx)

    // }
    when(hit) {
      if (assoc > 1) PLRU.access(plruBits.get(index), wayHitIdx)
      when(target <= pc) {
        branchTaken := true.B // btfn
      }.otherwise {
        branchTaken := false.B
      }

    }
  }

}
