// See README.md for license details.

package ex  // 按你的工程实际包名修改

import chisel3._
import chisel3.simulator.EphemeralSimulator._
import org.scalatest.freespec.AnyFreeSpec
import org.scalatest.matchers.must.Matchers

class Mux4to1_2bitVecSpec extends AnyFreeSpec with Matchers {

  "Mux4to1_2bitVec should index Vec inputs correctly" in {
    simulate(new top) { dut =>

      dut.reset.poke(true.B)
      dut.clock.step()
      dut.reset.poke(false.B)
      dut.clock.step()

      for {
        a <- 0 to 3
        b <- 0 to 3
        c <- 0 to 3
        d <- 0 to 3
        sel <- 0 to 3
      } {
        val inputs = Seq(a, b, c, d)

        dut.io.in(0).poke(a.U)
        dut.io.in(1).poke(b.U)
        dut.io.in(2).poke(c.U)
        dut.io.in(3).poke(d.U)
        dut.io.sel.poke(sel.U)

        dut.io.out.expect(inputs(sel).U)
        dut.clock.step()
      }
    }
  }
}
