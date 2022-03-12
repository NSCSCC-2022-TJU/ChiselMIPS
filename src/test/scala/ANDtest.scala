package test
 
object testMain extends App {
  (new chisel3.stage.ChiselStage).emitVerilog(new AND,args)
}