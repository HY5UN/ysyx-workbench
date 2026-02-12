package top

import chisel3._
import chisel3.util._

class InstFetchUnit extends ExtModule{
    val io =IO(new Bundle{
        val pc = Input(UInt(32.W))
        val inst = Output(UInt(32.W))
    })
}