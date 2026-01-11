package lab
import chisel3._
import chisel3.experimental.ExtModule

class sCPUROM extends ExtModule {
  val PC = IO(Input(UInt(4.W)))
  val inst = IO(Output(UInt(8.W)))
}
