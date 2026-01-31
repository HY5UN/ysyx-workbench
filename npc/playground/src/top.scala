package lab
import chisel3._
import chisel3.util._



class top extends Module {
  val io = IO(new Bundle {
    val ps2clk  = Input(Bool())
    val ps2data = Input(Bool())

    val hex  = Output(Vec(6, UInt(7.W)))
    // temp
    val led0 = Output(Bool())
    val state1 = Output(Bool())
    val state2 = Output(Bool())
    val state3 = Output(Bool())
  })

  val rx = Module(new PS2KeyboardRx)
  rx.io.ps2clk  := io.ps2clk
  rx.io.ps2data := io.ps2data

  val BreakCode = "hF0".U
  object State extends ChiselEnum {
    val sIdle, sPressed, sWaitReleaseCode = Value
  }
  import State._
  val state = RegInit(sIdle)
  val nextdata_nReg =RegInit(true.B)
  nextdata_nReg:=true.B
  //rx.io.nextdata_n:=nextdata_nReg
  rx.io.nextdata_n:= !rx.io.ready 

  val activeCode=RegInit(0.U(8.W))
  val keyCounter=RegInit(0.U(8.W))
  

  when(rx.io.ready){
    //nextdata_nReg:=false.B

    switch(state){
      is(sIdle){
        when(rx.io.data=/=BreakCode){
          state:=sPressed
          activeCode:=rx.io.data
          keyCounter:=keyCounter +1.U


        }
        .otherwise{
          
        }
      }
      is(sPressed){
        when(rx.io.data===BreakCode){
          state:=sWaitReleaseCode

        }

      }
      is(sWaitReleaseCode){
        when(rx.io.data===activeCode){

          
          state:=sIdle

        }
        .otherwise{
          state:=sWaitReleaseCode
        }

      }
    }


  }

  

  val asciiCode = Wire(UInt(8.W))
  asciiCode := MuxLookup(activeCode, 0.U(8.W))(
    Seq(
      // 数字 0~9 (Set2 make code)
      "h16".U -> "h31".U, // 1
      "h1E".U -> "h32".U, // 2
      "h26".U -> "h33".U, // 3
      "h25".U -> "h34".U, // 4
      "h2E".U -> "h35".U, // 5
      "h36".U -> "h36".U, // 6
      "h3D".U -> "h37".U, // 7
      "h3E".U -> "h38".U, // 8
      "h46".U -> "h39".U, // 9
      "h45".U -> "h30".U, // 0

      // 字母 a~z（Set2 make code）
      "h1C".U -> "h61".U, // a
      "h32".U -> "h62".U, // b
      "h21".U -> "h63".U, // c
      "h23".U -> "h64".U, // d
      "h24".U -> "h65".U, // e
      "h2B".U -> "h66".U, // f
      "h34".U -> "h67".U, // g
      "h33".U -> "h68".U, // h
      "h43".U -> "h69".U, // i
      "h3B".U -> "h6A".U, // j
      "h42".U -> "h6B".U, // k
      "h4B".U -> "h6C".U, // l
      "h3A".U -> "h6D".U, // m
      "h31".U -> "h6E".U, // n
      "h44".U -> "h6F".U, // o
      "h4D".U -> "h70".U, // p
      "h15".U -> "h71".U, // q
      "h2D".U -> "h72".U, // r
      "h1B".U -> "h73".U, // s
      "h2C".U -> "h74".U, // t
      "h3C".U -> "h75".U, // u
      "h2A".U -> "h76".U, // v
      "h1D".U -> "h77".U, // w
      "h22".U -> "h78".U, // x
      "h35".U -> "h79".U, // y
      "h1A".U -> "h7A".U  // z
    )
  )

  // val isKeyDown = (state===sPressed)||(state===sWaitReleaseCode)
  val isKeyDown = (state===sPressed)
  when(isKeyDown) {
    io.hex(0) := SevenSeg.encodeHex0toF(activeCode(3, 0), true.B)
    io.hex(1) := SevenSeg.encodeHex0toF(activeCode(7, 4), true.B)
    io.hex(2) := SevenSeg.encodeHex0toF(asciiCode(3, 0), true.B)
    io.hex(3) := SevenSeg.encodeHex0toF(asciiCode(7, 4), true.B)
  }.otherwise {
    io.hex(0) := SevenSeg.encodeHex0toF(0.U, false.B)
    io.hex(1) := SevenSeg.encodeHex0toF(0.U, false.B)
    io.hex(2) := SevenSeg.encodeHex0toF(0.U, false.B)
    io.hex(3) := SevenSeg.encodeHex0toF(0.U, false.B)
  }

  io.hex(4) := SevenSeg.encodeHex0toF(keyCounter(3, 0), true.B)
  io.hex(5) := SevenSeg.encodeHex0toF(keyCounter(7, 4), true.B)

  io.led0 := isKeyDown
  io.state1 := (state===sIdle)
  io.state2 := (state===sPressed)
  io.state3 := (state===sWaitReleaseCode)
}

// top=top

// io_ps2clk PS2_CLK
// io_ps2data PS2_DAT

// io_hex_0 (SEG0G,SEG0F,SEG0E,SEG0D,SEG0C,SEG0B,SEG0A)
// io_hex_1 (SEG1G,SEG1F,SEG1E,SEG1D,SEG1C,SEG1B,SEG1A)
// io_hex_2 (SEG2G,SEG2F,SEG2E,SEG2D,SEG2C,SEG2B,SEG2A)
// io_hex_3 (SEG3G,SEG3F,SEG3E,SEG3D,SEG3C,SEG3B,SEG3A)
// io_hex_4 (SEG4G,SEG4F,SEG4E,SEG4D,SEG4C,SEG4B,SEG4A)
// io_hex_5 (SEG5G,SEG5F,SEG5E,SEG5D,SEG5C,SEG5B,SEG5A)
// io_led0 LD0