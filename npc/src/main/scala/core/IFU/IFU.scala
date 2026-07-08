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
  // [修改点1] 将 1-bit dir 替换为 2-bit 饱和计数器
  val history = UInt(2.W) 
}

class BranchInfo extends Bundle {
  val pc     = UInt(32.W)
  val target = UInt(32.W)
  val dir    = UInt(1.W)
  val valid  = Bool()
  val taken  = Bool()
}

class IFU extends Module {
  val io = IO(new Bundle {
    val out        = Decoupled(new IFU2ICA)
    val redirectEn = Input(Bool())
    val redirectPc = Input(UInt(32.W))

    val branch = Input(new BranchInfo)
  })
  val pc          = RegInit("h30000000".U(32.W))
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

  val btb      = Reg(Vec(numGroups, Vec(assoc, new BTBEntry(tagWidth))))
  val validArr = RegInit(VecInit(Seq.fill(numGroups)(VecInit(Seq.fill(assoc)(false.B)))))

  // --- 读 (预测) 逻辑信号 ---
  val readIndex     = if (indexLen > 0) pc(indexLen + 1, 2) else 0.U
  val readTag       = pc(31, indexLen + 2)
  val readWayHitsOH = VecInit((0 until assoc).map(i => btb(readIndex)(i).tag === readTag && validArr(readIndex)(i)))
  val readWayDatas  = VecInit((0 until assoc).map(i => btb(readIndex)(i)))
  val readHit       = readWayHitsOH.asUInt.orR
  val entry         = Mux1H(readWayHitsOH, readWayDatas)
  val readWayHitIdx = OHToUInt(readWayHitsOH)

  val plruBits =
    if (assoc > 1) Some(RegInit(VecInit(Seq.fill(numGroups)(VecInit(Seq.fill(assoc - 1)(false.B)))))) else None

  // --- 写 (更新) 逻辑信号 ---
  val writeIndex      = if (indexLen > 0) branchReg.pc(indexLen + 1, 2) else 0.U
  val writeTag        = branchReg.pc(31, indexLen + 2)
  
  // [修改点2] 增加写命中检查及命中路索引计算，用于更新状态机
  val writeWayHitsOH  = VecInit((0 until assoc).map(i => btb(writeIndex)(i).tag === writeTag && validArr(writeIndex)(i)))
  val writeHit        = writeWayHitsOH.asUInt.orR
  val writeWayHitIdx  = OHToUInt(writeWayHitsOH)
  val writeReplaceWay = if (assoc > 1) PLRU.victim(plruBits.get(writeIndex)) else 0.U

  // 处理跳转
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

  // [修改点3] 更新BTB与2-bit状态机 (移除旧的仅taken更新逻辑，改为每次决议都更新)
  when(branchReg.valid && updateBTB) {
    when(writeHit) {
      // 命中：根据实际跳转结果更新2-bit饱和计数器
      val currentHist = btb(writeIndex)(writeWayHitIdx).history
      btb(writeIndex)(writeWayHitIdx).history := Mux(branchReg.taken,
        Mux(currentHist === 3.U, 3.U, currentHist + 1.U), // Taken: 状态递增，饱和在3(11)
        Mux(currentHist === 0.U, 0.U, currentHist - 1.U)  // Not Taken: 状态递减，饱和在0(00)
      )
      
      // 更新PLRU状态
      if (assoc > 1) PLRU.access(plruBits.get(writeIndex), writeWayHitIdx)

    }.elsewhen(branchReg.taken) {
      // 未命中，且实际发生跳转：分配新表项
      validArr(writeIndex)(writeReplaceWay)   := true.B
      btb(writeIndex)(writeReplaceWay).tag    := writeTag
      btb(writeIndex)(writeReplaceWay).target := branchReg.target
      btb(writeIndex)(writeReplaceWay).history := 2.U // 初始化为 10 (弱预测跳转)
      
      if (assoc > 1) PLRU.access(plruBits.get(writeIndex), writeReplaceWay)
    }

    branchReg.valid := false.B
    updateBTB       := false.B
  }.otherwise {
    when(readHit) {
      if (assoc > 1) PLRU.access(plruBits.get(readIndex), readWayHitIdx)

      // [修改点4] 基于2-bit饱和计数器的预测逻辑
      // 只要最高位是1 (即状态 10 或 11)，就预测跳转
      when(entry.history(1).asBool) {
        branchTaken := true.B
      }.otherwise {
        branchTaken := false.B
      }
    }
  }
}