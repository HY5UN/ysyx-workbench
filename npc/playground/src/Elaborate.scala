
package playground
import chisel3.stage.ChiselStage

object Elaborate extends App {
  (new ChiselStage).emitVerilog(new Mux21, Array("--target-dir", "build"))
}