class AXI4LiteIO extends Bundle {
  // ---------- Read Address Channel ----------
  val araddr  = Output(UInt(32.W))
  val arvalid = Output(Bool())
  val arready = Input(Bool())
 
  // ---------- Read Data Channel -------------
  val rdata  = Input(UInt(32.W))
  val rresp  = Input(UInt(2.W))
  val rvalid = Input(Bool())
  val rready = Output(Bool())
 
  // ---------- Write Address Channel ---------
  val awaddr  = Output(UInt(32.W))
  val awvalid = Output(Bool())
  val awready = Input(Bool())
 
  // ---------- Write Data Channel ------------
  val wdata  = Output(UInt(32.W))
  val wstrb  = Output(UInt(4.W))
  val wvalid = Output(Bool())
  val wready = Input(Bool())
 
  // ---------- Write Response Channel --------
  val bresp  = Input(UInt(2.W))
  val bvalid = Input(Bool())
  val bready = Output(Bool())
}
