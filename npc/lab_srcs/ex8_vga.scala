package lab
import chisel3._
import chisel3.util._

class top extends Module {
  val io = IO(new Bundle {
    val vgaVsync = Output(Bool())
    val vgaHsync = Output(Bool())
    val vgaBlank_n= Output(Bool())
    val vgaR      = Output(UInt(8.W))
    val vgaG      = Output(UInt(8.W))
    val vgaB      = Output(UInt(8.W))
  })

 


  val clkGen = Module(new ClkGen(25_000_000))
  clkGen.io.clkEn:=true.B
  clkGen.io.clkIn:=clock

  val RAM = Module(new VgaMem)

  withClock(clock) {
    val vc = Module(new VgaCtrl)
    
    val ramAddr = Cat(vc.io.hAddr, vc.io.vAddr(8,0))  
    RAM.addr := ramAddr
    vc.io.vgaData := RAM.data

    io.vgaHsync := vc.io.hsync
    io.vgaVsync := vc.io.vsync
    io.vgaBlank_n :=vc.io.valid
    io.vgaR := vc.io.vgaR
    io.vgaG := vc.io.vgaG
    io.vgaB := vc.io.vgaB
   
  }



}

// top=top

// io_vgaVsync VGA_VSYNC
// io_vgaHsync VGA_HSYNC
// io_vgaBlank_n VGA_BLANK_N
// io_vgaR (VGA_R7, VGA_R6, VGA_R5, VGA_R4, VGA_R3, VGA_R2, VGA_R1, VGA_R0)
// io_vgaG (VGA_G7, VGA_G6, VGA_G5, VGA_G4, VGA_G3, VGA_G2, VGA_G1, VGA_G0)
// io_vgaB (VGA_B7, VGA_B6, VGA_B5, VGA_B4, VGA_B3, VGA_B2, VGA_B1, VGA_B0)