package lab
import chisel3._
import chisel3.util._

class VgaCtrl extends Module {
  val io = IO(new Bundle {
    val vgaData = Input(UInt(24.W))
    val hAddr   = Output(UInt(10.W))
    val vAddr   = Output(UInt(10.W))
    val hsync   = Output(Bool())
    val vsync   = Output(Bool())
    val valid   = Output(Bool())
    val vgaR    = Output(UInt(8.W))
    val vgaG    = Output(UInt(8.W))
    val vgaB    = Output(UInt(8.W))
  })

  val hFrontPorch = 96.U
  val hActive     = 144.U
  val hBackPorch  = 784.U
  val hTotal      = 800.U

  val vFrontPorch = 2.U
  val vActive     = 35.U
  val vBackPorch  = 515.U
  val vTotal      = 525.U

  val xCnt = RegInit(0.U(10.W))
  val yCnt = RegInit(0.U(10.W))

  io.hsync := !(xCnt < hFrontPorch)
  io.vsync := !(yCnt < vFrontPorch)

  val hValid = (xCnt >= hActive) && (xCnt < hBackPorch)
  val vValid = (yCnt >= vActive) && (yCnt < vBackPorch)
  io.valid := hValid && vValid

  io.hAddr := Mux(hValid, xCnt - hActive, 0.U)
  io.vAddr := Mux(vValid, yCnt - vActive, 0.U)

  io.vgaR := io.vgaData(23, 16)
  io.vgaG := io.vgaData(15, 8)
  io.vgaB := io.vgaData(7, 0)

  when(xCnt === (hTotal - 1.U)) {
    xCnt := 0.U
    when(yCnt === (vTotal - 1.U)) {
      yCnt := 0.U
    }.otherwise {
      yCnt := yCnt + 1.U
    }
  }.otherwise {
    xCnt := xCnt + 1.U
  }

}
