package top
import chisel3._
import chisel3.reflect.DataMirror
import chisel3.ActualDirection

object ChiselUtils {
  // 定义隐式类，为所有 Chisel Data 类型添加扩展方法
  implicit class DataZeroExtension(val data: Data) extends AnyVal {
    def driveZeroOutputs(): Unit = {
      data match {
        // 如果是 Bundle (Record)
        case r: Record => r.elements.values.foreach(_.driveZeroOutputs())
        // 如果是 数组 (Vec)
        case v: Vec[_] => v.foreach(_.driveZeroOutputs())
        // 如果是 基础信号 (UInt, SInt, Bool 等都继承自 Bits)
        case b: Bits =>
          // 通过 DataMirror 获取实际方向，如果是 Output 则驱动为 0
          if (DataMirror.directionOf(b) == ActualDirection.Output) {
            b := 0.U
          }
        case _ => // 其他类型不做处理
      }
    }
  }
}