package lab
import chisel3._
import chisel3.experimental.ExtModule

class VgaMem extends ExtModule {
  val addr = IO(Input(UInt(19.W)))
  val data = IO(Output(UInt(24.W)))
}
