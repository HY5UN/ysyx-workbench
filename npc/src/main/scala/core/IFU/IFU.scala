// package top

// import chisel3._
// import chisel3.util._

// class IFU2IDU extends Bundle {
//   val inst     = UInt(32.W)
//   val pc       = UInt(32.W)
//   val pc4 = UInt(32.W)
//   val excValid = Bool()
//   val excType  = ExceptionType()
//   val dpic_tag  = UInt(8.W)
// }

// class IFU_old extends Module {
//   val io = IO(new Bundle {
//     val out           = Decoupled(new IFU2IDU)
//     val axi           = new AXI4IO
//     val flush         = Input(Bool())
//     val nextPc        = Input(UInt(32.W))
//     val pfm_miss      = Output(Bool())
//     val pfm_i_flushed = Output(Bool())
//   })

//   // val araddrReg = RegInit("h80000000".U(32.W))
//   val araddrReg = RegInit("h30000000".U(32.W))
//   object State extends ChiselEnum {
//     val sIdle, sPcWait = Value
//   }
//   val state = RegInit(State.sIdle)
//   val icache = Module(new ICache(cacheSizeB = 128, blockSizeB = 16, assoc = 2))
//   icache.io.axi <> io.axi
//   icache.io.ifu.pc      := araddrReg
//   icache.io.ifu.pcValid := false.B
//   icache.io.ifu.fencei  := false.B

//   val flushReg  = RegEnable(io.flush, io.flush)
//   val nextPcReg = RegEnable(io.nextPc, io.flush)

//   val excValidReg = RegInit(false.B)
//   io.out.bits.excValid := excValidReg
//   io.out.bits.excType  := ExceptionType.InstructionAccessFault

//   io.out.bits.inst := icache.io.ifu.inst
//   io.out.bits.pc   := araddrReg
//   io.out.valid     := false.B

//   val dpic_tagReg = Reg(UInt(8.W))
//   io.pfm_miss      := false.B
//   io.pfm_i_flushed := false.B
//   val pc4  = WireInit((araddrReg + 4.U)(31,0))
//   io.out.bits.pc4:= pc4
//   switch(state) {

//     is(State.sIdle) {
//       when(flushReg || io.flush) {
//         flushReg         := false.B
//         araddrReg        := Mux(io.flush, io.nextPc, nextPcReg)
//         io.pfm_i_flushed := icache.io.ifu.instValid
//       }.otherwise {
//         icache.io.ifu.pcValid := true.B
//         when(io.out.fire) {
//           araddrReg  := pc4
//           dpic_tagReg := dpic_tagReg + 1.U

//           excValidReg := false.B
//         }
//         when(icache.io.ifu.instValid) {
//           io.out.valid := true.B
//         }.otherwise {
//           io.pfm_miss := true.B
//           state       := State.sPcWait
//         }
//       }
//     }
//     is(State.sPcWait) {
//       icache.io.ifu.pcValid := true.B
//       when(icache.io.ifu.instValid) {
//         state := State.sIdle
//         when(icache.io.ifu.err) {
//           excValidReg := true.B
//         }
//       }
//     }
//   }

//   io.out.bits.dpic_tag := dpic_tagReg
// }
