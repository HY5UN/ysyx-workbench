
package playground
import chisel3.stage.ChiselStage

object Elaborate extends App {
  (new ChiselStage).emitVerilog(
    new M_mux21(), 
    args
  )
}