// package lab
// import chisel3._
// import chisel3.util._

// class top extends Module {
//   val io = IO(new Bundle {
//     val ps2clk  = Input(Bool())
//     val ps2data = Input(Bool())

//     val hex     = Output(Vec(6, UInt(7.W)))
//     // temp
//     val keydown = Output(Bool())
//   })

//   val rx = Module(new PS2KeyboardRx)
//   rx.io.ps2clk  := io.ps2clk
//   rx.io.ps2data := io.ps2data

//   val keydownReg = RegInit(false.B)

//   val gotByte = RegInit(false.B)
//   val dataReg = RegInit(0.U(8.W))
//   val nextdata_nReg = RegInit(true.B) 
//   nextdata_nReg := true.B
//   rx.io.nextdata_n := nextdata_nReg
//   val readyReg = RegInit( false.B)
//   when(!readyReg && rx.io.ready){
//     readyReg := true.B
//   }
//   when(readyReg){
//     gotByte := true.B
//     dataReg := rx.io.data
//     nextdata_nReg := false.B
//     readyReg := true.B
//   }

  

//   when(gotByte) {
//     when(rx.io.data === "hF0".U) {
//       keydownReg := false.B
//     }.otherwise {
//       when(keydownReg === false.B) {
//         keydownReg := true.B

//       }
//     }
//     gotByte := false.B
//   }
//   //io.keydown := keydownReg
//   io.keydown := readyReg
//   when(keydownReg) {
//     io.hex(0) := SevenSeg.encodeHex0toF(dataReg(3, 0), true.B)
//     io.hex(1) := SevenSeg.encodeHex0toF(dataReg(7, 4), true.B)
//   }.otherwise {
//     io.hex(0) := SevenSeg.encodeHex0toF(0.U, false.B)
//     io.hex(1) := SevenSeg.encodeHex0toF(0.U, false.B)
//   }
//   for (i <- 2 until 6) {
//     io.hex(i) := SevenSeg.encodeHex0toF(0.U, false.B)
//   }

// }


package lab

import chisel3._
import chisel3.util._

class top extends Module {
  val io = IO(new Bundle {
    // ===== PS/2 接口 =====
    val ps2clk   = Input(Bool())
    val ps2data  = Input(Bool())

    // ===== 数码管 =====
    val hex_0 = Output(UInt(7.W))
    val hex_1 = Output(UInt(7.W))
    val hex_2 = Output(UInt(7.W))
    val hex_3 = Output(UInt(7.W))
    val hex_4 = Output(UInt(7.W))
    val hex_5 = Output(UInt(7.W))

    // ===== LED =====
    val keydown = Output(Bool())
  })

  // ===============================
  // PS/2 接收模块
  // ===============================
  val ps2 = Module(new PS2KeyboardRx)

  ps2.io.ps2clk     := io.ps2clk
  ps2.io.ps2data    := io.ps2data
  ps2.io.nextdata_n := false.B   // 自动读取，不做握手

  // ===============================
  // 数据锁存（仅在 ready 时更新）
  // ===============================
  val dataReg = RegInit(0.U(8.W))
  when(ps2.io.ready) {
    dataReg := ps2.io.data
  }

  // ===============================
  // 数码管显示
  // 显示格式：
  // HEX5 HEX4 HEX3 HEX2 HEX1 HEX0
  //  0    0    0    D7D6 D5D4 D3D2
  // ===============================
  io.hex_0 := encodeHex0toF(dataReg(3, 0),  ps2.io.ready)
  io.hex_1 := encodeHex0toF(dataReg(7, 4),  ps2.io.ready)
  io.hex_2 := encodeHex0toF(0.U,             false.B)
  io.hex_3 := encodeHex0toF(0.U,             false.B)
  io.hex_4 := encodeHex0toF(0.U,             false.B)
  io.hex_5 := encodeHex0toF(0.U,             false.B)

  // ===============================
  // LED：有新键码时亮
  // ===============================
  io.keydown := ps2.io.ready
}
