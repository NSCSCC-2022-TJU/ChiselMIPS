package Pipeline
 
object testCMP extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new CMP,args)
}