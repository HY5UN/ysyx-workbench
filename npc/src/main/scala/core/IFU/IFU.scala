package top
import chisel3._
import chisel3.util._

class IFU2ICA extends Bundle {
  val pc             = UInt(32.W)
  val pc4            = UInt(32.W)
  val branchPreTaken = Bool()
  val dpic_tag       = UInt(8.W)

}

class BTBEntry(tagWidth: Int) extends Bundle {
  val tag     = UInt(tagWidth.W)
  val target  = UInt(32.W)
  val dir = UInt(1.W)
}

class BranchInfo extends Bundle {
  val pc     = UInt(32.W)
  val target = UInt(32.W)
  val dir = UInt(1.W)
  val valid  = Bool()
  val taken  = Bool()
}

class IFU extends Module {
  val io          = IO(new Bundle {
    val out        = Decoupled(new IFU2ICA)
    val redirectEn = Input(Bool())
    val redirectPc = Input(UInt(32.W))

    val branch = Input(new BranchInfo)
  })
  val pc          = RegInit("h30000000".U(32.W))
  // val pc          = RegInit("h80000000".U(32.W))
  val pc4         = WireInit((pc + 4.U)(31, 0))
  val dpic_tagReg = RegInit(0.U(8.W))
  val updateBTB   = RegInit(false.B)

  io.out.bits.pc       := pc
  io.out.bits.pc4      := pc4
  io.out.bits.dpic_tag := dpic_tagReg
  io.out.valid         := false.B

  // 保存跳转信息
  val branchReg = RegEnable(io.branch, io.branch.valid)

  // BTB参数计算
  val numEntries = 4
  val assoc      = 4
  val numGroups  = numEntries / assoc
  val indexLen   = log2Ceil(numGroups)
  val tagWidth   = 32 - (indexLen + 2)

  // ==========================================
  // 1. 预测路径（读）：无条件直接使用当前 PC
  // ==========================================
  val readIndex = if (indexLen > 0) pc(indexLen + 1, 2) else 0.U
  val readTag   = pc(31, indexLen + 2)

  val btb      = Reg(Vec(numGroups, Vec(assoc, new BTBEntry(tagWidth))))
  val validArr = RegInit(VecInit(Seq.fill(numGroups)(VecInit(Seq.fill(assoc)(false.B)))))

  // 读命中的比较逻辑位于关键路径上，现在它完全脱离了 updateBTB 的控制
  val readWayHitsOH = VecInit((0 until assoc).map(i => btb(readIndex)(i).tag === readTag && validArr(readIndex)(i)))
  val readWayDatas  = VecInit((0 until assoc).map(i => btb(readIndex)(i)))
  val readHit       = readWayHitsOH.asUInt.orR
  val entry         = Mux1H(readWayHitsOH, readWayDatas)

  val plruBits      =
    if (assoc > 1) Some(RegInit(VecInit(Seq.fill(numGroups)(VecInit(Seq.fill(assoc - 1)(false.B)))))) else None
  val readWayHitIdx = OHToUInt(readWayHitsOH)

  // ==========================================
  // 2. 更新路径（写）：使用后端传回的 branchReg.pc
  // ==========================================
  val writeIndex = if (indexLen > 0) branchReg.pc(indexLen + 1, 2) else 0.U
  val writeTag   = branchReg.pc(31, indexLen + 2)

  // 为了保持你原本的逻辑，需要判断要更新的地址是否已经存在于 BTB 中 (写命中检查)
  // 这部分逻辑不在关键路径上，不会影响频率
  val writeWayHitsOH  = VecInit((0 until assoc).map(i => btb(writeIndex)(i).tag === writeTag && validArr(writeIndex)(i)))
  val writeHit        = writeWayHitsOH.asUInt.orR
  val writeReplaceWay = if (assoc > 1) PLRU.victim(plruBits.get(writeIndex)) else 0.U

  // ==========================================
  // 3. 处理跳转 (保留你原本的 imm + pc 逻辑)
  // ==========================================
  val branchTaken  = WireInit(false.B)
  val branchNextPc = WireInit(entry.target) 
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

  // ==========================================
  // 4. 状态更新逻辑 (分离读写条件)
  // ==========================================
  when(branchReg.valid && updateBTB && branchReg.taken) {
    // 使用专用的 writeHit 和 writeIndex 进行判断和写入
    when(!writeHit) {
      validArr(writeIndex)(writeReplaceWay)    := true.B
      btb(writeIndex)(writeReplaceWay).tag     := writeTag
      btb(writeIndex)(writeReplaceWay).target  := branchReg.target
      btb(writeIndex)(writeReplaceWay).dir := branchReg.dir
      if (assoc > 1) PLRU.access(plruBits.get(writeIndex), writeReplaceWay)
    }

    branchReg.valid := false.B
    updateBTB       := false.B
  }.otherwise {
    // 只有在正常取指（非更新）时，才触发读命中的状态更新 (PLRU 记录等)
    when(readHit) {
      if (assoc > 1) PLRU.access(plruBits.get(readIndex), readWayHitIdx)

      /// btfn 逻辑
      when(entry.dir.asBool) {
        branchTaken := true.B
      }.otherwise {
        branchTaken := false.B
      }
    }
  }

}
