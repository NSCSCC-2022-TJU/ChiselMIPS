package Pipeline
 
object testMain extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new ALU,args)
}