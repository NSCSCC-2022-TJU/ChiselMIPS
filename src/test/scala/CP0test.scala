package Pipeline
 
object testCP0 extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new CP0,args)
}