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

  withClock(clkGen.io.clkOut) {
    val vc = Module(new VgaCtrl)
    
    val ramAddr = Cat(vc.io.hAddr, vc.io.vAddr(8,0))  
    RAM.io.addr := ramAddr
    vc.io.vgaData := RAM.io.data

    io.vgaHsync := vc.io.hsync
    io.vgaVsync := vc.io.vsync
    io.vgaBlank_n := vc.io.valid
    io.vgaR := vc.io.vgaR
    io.vgaG := vc.io.vgaG
    io.vgaB := vc.io.vgaB
   
  }



}