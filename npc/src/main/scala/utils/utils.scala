package top
import chisel3._
import chisel3.reflect.DataMirror
import chisel3.ActualDirection

object ChiselUtils {

  // 接收一个 Data 类型的参数
  def driveZeroOutputs(data: Data): Unit = {
    data match {
      // 递归调用时，也要用标准的函数调用
      case r: Record => r.elements.values.foreach(driveZeroOutputs)
      case v: Vec[_] => v.foreach(driveZeroOutputs)
      case b: Bits   =>
        if (DataMirror.directionOf(b) == ActualDirection.Output) {
          b := 0.U
        }
      case _ =>
    }
  }

}

import chisel3._

object BundleConnect {
  def apply(source: Bundle, sink: Bundle): Unit = {
    source.elements.foreach { case (name, sourceData) =>
      if (sink.elements.contains(name)) {
        sink.elements(name) := sourceData
      }
    }
  }
}
