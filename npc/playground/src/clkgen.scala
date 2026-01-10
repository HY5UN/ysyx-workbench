package  lab
import chisel3._
import chisel3.util._

class ClkGen (val clkFreqHz:Int, val inFreqHz:Int =50_000_000) extends Module{
    val io = IO (new Bundle {
        val clkIn  = Input  (Bool())
        val clkEn  = Input  (Bool())
        val clkOut = Output (Bool())
    })

    val countLimit:Long = (inFreqHz.toLong / 2L) / clkFreqHz.toLong
    val cntWidth = log2Ceil(countLimit.toInt + 1).max(1)
    val clkCount = RegInit(0.U(cntWidth.W))
    val clkOutReg = RegInit(false.B)

    when (io.clkEn) {
        when (clkCount >= (countLimit - 1).U) {
            clkCount := 0.U
            clkOutReg := ~clkOutReg
        } .otherwise {
            clkCount := clkCount + 1.U
        }
    }  
    io.clkOut := clkOutReg
}