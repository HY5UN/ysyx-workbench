package lab
import chisel3._
import chisel3.util._

object SevenSeg {
  private val table = VecInit(
    Seq(
      "b1000000".U(7.W), // 0
      "b1111001".U(7.W), // 1
      "b0100100".U(7.W), // 2
      "b0110000".U(7.W), // 3
      "b0011001".U(7.W), // 4
      "b0010010".U(7.W), // 5
      "b0000010".U(7.W), // 6
      "b1111000".U(7.W)  // 7
    )
  )

  def encodeDigit0to7(digit: UInt): UInt = {
    
    table(digit)
  } 

}
