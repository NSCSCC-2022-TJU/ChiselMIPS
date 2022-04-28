package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

/*
  !!! 已作废
 */
class CoreWrapper(implicit config: Config) extends RawModule {
  override def desiredName = "MiniMIPS32"

  val cpu_clk_50M = IO(Input(Clock()))
  val cpu_rst_n   = IO(inputBool)

  val iaddr             = IO(outputUInt(machine_width))
  val inst_req          = IO(outputBool)
  val inst              = IO(inputUInt(machine_width))
  val iaddr_ok          = IO(inputBool)
  val dce               = IO(outputBool)
  val daddr             = IO(outputUInt(machine_width))
  val we                = IO(outputUInt(4))
  val din               = IO(outputUInt(machine_width))
  val dm                = IO(inputUInt(machine_width))
  val if_data_ok        = IO(inputBool)
  val mem_data_ok       = IO(inputBool)
  val int               = IO(inputUInt(int_width))
  val debug_wb_pc       = IO(outputUInt(machine_width))
  val debug_wb_rf_wen   = IO(outputUInt(4))
  val debug_wb_rf_wnum  = IO(outputUInt(regAddr_width))
  val debug_wb_rf_wdata = IO(outputUInt(machine_width))

  withClockAndReset(cpu_clk_50M, !cpu_rst_n) {
    val core   = Module(new Core)
    val coreio = core.io

//    iaddr <> coreio.cacheIF.instAddr
    inst_req <> coreio.cacheIF.instReq
    inst <> coreio.cacheID.inst
    iaddr_ok <> coreio.cacheIF.instAddrOK
    dce <> coreio.cacheMEM.req
//    daddr <> coreio.cacheMEM.daddr
    we <> coreio.cacheMEM.we
    din <> coreio.cacheMEM.din
    dm <> coreio.cacheWB.rdata
    if_data_ok <> coreio.cacheIF.dataOK
    // todo 优化
    mem_data_ok <> coreio.cacheMEM.dataOk
    mem_data_ok <> coreio.cacheWB.dataOk

    int <> coreio.int
    debug_wb_pc <> coreio.referencePC
    debug_wb_rf_wen <> coreio.referenceWEN
    debug_wb_rf_wnum <> coreio.referenceWNUM
    debug_wb_rf_wdata <> coreio.referenceWDATA
  }
}

object CoreWrapper extends App {
  implicit val config = new Config
  println(getVerilogString(new CoreWrapper()))
//   (new ChiselStage).emitVerilog(new CoreWrapper(), Array("--target-dir", "output/"))
   (new ChiselStage).emitVerilog(new CoreWrapper(), Array("--target-dir", "E:\\A_loong\\func_test_v0.01\\soc_axi_func\\rtl\\myCPU\\"))
}
