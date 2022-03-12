package Pipeline
 
object testDataCheck extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new DataCheck,args)
}