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

  def encodeDigit0to7(digit: UInt, valid: Bool): UInt = {
    Mux(valid, table(digit), "b1111111".U(7.W))
  } 

  def encodeHex0toF(hex: UInt, valid: Bool): UInt = {
    val hexTable = VecInit(
      Seq(
        "b1000000".U(7.W), // 0
        "b1111001".U(7.W), // 1
        "b0100100".U(7.W), // 2
        "b0110000".U(7.W), // 3
        "b0011001".U(7.W), // 4
        "b0010010".U(7.W), // 5
        "b0000010".U(7.W), // 6
        "b1111000".U(7.W), // 7
        "b0000000".U(7.W), // 8
        "b0010000".U(7.W), // 9
        "b0001000".U(7.W), // A
        "b0000011".U(7.W), // B
        "b1000110".U(7.W), // C
        "b0100001".U(7.W), // D
        "b0000110".U(7.W), // E
        "b0001110".U(7.W)  // F
      )
    )
    Mux(valid, hexTable(hex), "b1111111".U(7.W))
  }

}
