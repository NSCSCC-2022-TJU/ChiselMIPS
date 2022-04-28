package mips

import chisel3._
import chisel3.experimental.dataview.DataView
import chisel3.util._
import chisel3.stage.ChiselStage
import config.Config._
import config._

//class SysTop_IO() extends Bundle {
//  val clk               = Input(Clock())
//  val reset             = inputBool
//  val int               = inputUInt(int_width)
//  val axi               = new AXIMaster_IO
//  val debug_wb_pc       = outputUInt(machine_width)
//  val debug_wb_rf_wen   = outputUInt(4)
//  val debug_wb_rf_wnum  = outputUInt(regAddr_width)
//  val debug_wb_rf_wdata = outputUInt(machine_width)
//}
//
//class VerilogSysTop_IO() extends Bundle {
//  val aclk    = Input(Clock())
//  val aresetn = inputBool
//  val ext_int = inputUInt(int_width)
//
//  val arid    = outputUInt(4)
//  val araddr  = outputUInt(machine_width)
//  val arlen   = outputUInt(8)
//  val arsize  = outputUInt(3)
//  val arburst = outputUInt(2)
//  val arlock  = outputUInt(2)
//  val arcache = outputUInt(4)
//  val arprot  = outputUInt(3)
//  val arvalid = outputBool
//  val arready = inputBool
//
//  val rid     = inputUInt(4)
//  val rdata   = inputUInt(machine_width)
//  val rresp   = inputUInt(2)
//  val rlast   = inputBool
//  val rvalid  = inputBool
//  val rready  = outputBool
//  val awid    = outputUInt(4)
//  val awaddr  = outputUInt(machine_width)
//  val awlen   = outputUInt(8)
//  val awsize  = outputUInt(3)
//  val awburst = outputUInt(2)
//  val awlock  = outputUInt(2)
//  val awcache = outputUInt(4)
//  val awprot  = outputUInt(3)
//  val awvalid = outputBool
//  val awready = inputBool
//
//  val wid    = outputUInt(4)
//  val wdata  = outputUInt(machine_width)
//  val wstrb  = outputUInt(4)
//  val wlast  = outputBool
//  val wvalid = outputBool
//  val wready = inputBool
//
//  val bid    = inputUInt(4)
//  val bresp  = inputUInt(2)
//  val bvalid = inputBool
//  val bready = outputBool
//
//  val debug_wb_pc       = outputUInt(machine_width)
//  val debug_wb_rf_wen   = outputUInt(4)
//  val debug_wb_rf_wnum  = outputUInt(regAddr_width)
//  val debug_wb_rf_wdata = outputUInt(machine_width)
//}

class SysTop(implicit config: Config) extends RawModule {
  override def desiredName = "mycpu_top"

  val aclk    = IO(Input(Clock()))
  val aresetn = IO(inputBool)
  val ext_int = IO(inputUInt(int_width))

  val arid    = IO(outputUInt(4))
  val araddr  = IO(outputUInt(machine_width))
  val arlen   = IO(outputUInt(8))
  val arsize  = IO(outputUInt(3))
  val arburst = IO(outputUInt(2))
  val arlock  = IO(outputUInt(2))
  val arcache = IO(outputUInt(4))
  val arprot  = IO(outputUInt(3))
  val arvalid = IO(outputBool)
  val arready = IO(inputBool)
  val rid     = IO(inputUInt(4))
  val rdata   = IO(inputUInt(machine_width))
  val rresp   = IO(inputUInt(2))
  val rlast   = IO(inputBool)
  val rvalid  = IO(inputBool)
  val rready  = IO(outputBool)
  val awid    = IO(outputUInt(4))
  val awaddr  = IO(outputUInt(machine_width))
  val awlen   = IO(outputUInt(8))
  val awsize  = IO(outputUInt(3))
  val awburst = IO(outputUInt(2))
  val awlock  = IO(outputUInt(2))
  val awcache = IO(outputUInt(4))
  val awprot  = IO(outputUInt(3))
  val awvalid = IO(outputBool)
  val awready = IO(inputBool)
  val wid     = IO(outputUInt(4))
  val wdata   = IO(outputUInt(machine_width))
  val wstrb   = IO(outputUInt(4))
  val wlast   = IO(outputBool)
  val wvalid  = IO(outputBool)
  val wready  = IO(inputBool)
  val bid     = IO(inputUInt(4))
  val bresp   = IO(inputUInt(2))
  val bvalid  = IO(inputBool)
  val bready  = IO(outputBool)

  val debug_wb_pc       = IO(outputUInt(machine_width))
  val debug_wb_rf_wen   = IO(outputUInt(4))
  val debug_wb_rf_wnum  = IO(outputUInt(regAddr_width))
  val debug_wb_rf_wdata = IO(outputUInt(machine_width))

  withClockAndReset(aclk, !aresetn) {
    val stageIF  = Module(new StageIF())
    val stageID  = Module(new StageID())
    val stageEXE = Module(new StageEXE())
    val stageMEM = Module(new StageMEM())
    val stageWB  = Module(new StageWB())
    val cp0      = Module(new CP0())
    val regFile  = Module(new RegFile)
    val scu      = Module(new SCU)
    val icache   = if (config.hasCache) Some(Module(new ICache())) else None
    val dcache   = if (config.hasCache) Some(Module(new DCache())) else None
    val mmu      = Module(new MMU())
    val sram2axi = Module(new SRAM2AXI())

    stageIF.io.idio <> stageID.io.ifio
    stageID.io.exeio <> stageEXE.io.idio
    stageID.io.memio <> stageMEM.io.idio
    stageEXE.io.memio <> stageMEM.io.exeio
    stageEXE.io.wbio <> stageWB.io.exeio
    stageMEM.io.wbio <> stageWB.io.memio

    // regFile
    stageID.io.regfileio.raddr1 <> regFile.io.raddr1
    stageID.io.regfileio.raddr2 <> regFile.io.raddr2
    stageID.io.regfileio.rdata1 <> regFile.io.rdata1
    stageID.io.regfileio.rdata2 <> regFile.io.rdata2
    stageWB.io.regfileio.wen <> regFile.io.wen
    stageWB.io.regfileio.wdata <> regFile.io.wdata
    stageWB.io.regfileio.waddr <> regFile.io.waddr

    // cp0
    stageIF.io.cp0io <> cp0.io.ifio
    stageEXE.io.cp0io <> cp0.io.exeio
    stageMEM.io.cp0io <> cp0.io.memio
    stageWB.io.cp0io <> cp0.io.wbio
    cp0.io.int <> ext_int
    cp0.io.flushio <> stageIF.io.flushio
    cp0.io.flushio <> stageID.io.flushio
    cp0.io.flushio <> stageEXE.io.flushio
    cp0.io.flushio <> stageMEM.io.flushio
    cp0.io.flushio.flush <> stageWB.io.flush

    // scu
    stageIF.io.scuio.stallReq <> scu.io.stallReqIF
    stageIF.io.scuio.stall <> scu.io.stall
    stageID.io.scuio.stallReq <> scu.io.stallReqID
    stageID.io.scuio.stall <> scu.io.stall
    stageEXE.io.scuio.stallReq <> scu.io.stallReqEXE
    stageEXE.io.scuio.stall <> scu.io.stall
    stageMEM.io.scuio.stallReq <> scu.io.stallReqMEM
    stageMEM.io.scuio.stall <> scu.io.stall
    stageWB.io.stall <> scu.io.stall

    // mmu
    mmu.io.ifio <> stageIF.io.mmuio
    mmu.io.memio <> stageMEM.io.mmuio
    if (config.hasTLB) {
      mmu.io.wbio.get <> stageWB.io.mmuio.get
      mmu.io.cp0io.get <> cp0.io.mmuio.get
    }


    if (config.hasCache) {
      val icacheio = icache.get.io
      val dcacheio = dcache.get.io
      val axiio    = sram2axi.io

      // icache
      icacheio.ifio <> stageIF.io.cacheio
      icacheio.idio <> stageID.io.cacheio
      icacheio.mmuio <> mmu.io.icacheio
      // dcache
      dcacheio.memio <> stageMEM.io.cacheio
      dcacheio.wbio <> stageWB.io.cacheio
      dcacheio.mmuio <> mmu.io.dcacheio
      // axi
      axiio.icacheio <> icacheio.crossbario
      axiio.dcacheio <> dcacheio.crossbario
      axiio.axiio.arid <> arid
      axiio.axiio.araddr <> araddr
      axiio.axiio.arlen <> arlen
      axiio.axiio.arsize <> arsize
      axiio.axiio.arburst <> arburst
      axiio.axiio.arlock <> arlock
      axiio.axiio.arcache <> arcache
      axiio.axiio.arprot <> arprot
      axiio.axiio.arvalid <> arvalid
      axiio.axiio.arready <> arready
      axiio.axiio.rid <> rid
      axiio.axiio.rdata <> rdata
      axiio.axiio.rresp <> rresp
      axiio.axiio.rlast <> rlast
      axiio.axiio.rvalid <> rvalid
      axiio.axiio.rready <> rready
      axiio.axiio.awid <> awid
      axiio.axiio.awaddr <> awaddr
      axiio.axiio.awlen <> awlen
      axiio.axiio.awsize <> awsize
      axiio.axiio.awburst <> awburst
      axiio.axiio.awlock <> awlock
      axiio.axiio.awcache <> awcache
      axiio.axiio.awprot <> awprot
      axiio.axiio.awvalid <> awvalid
      axiio.axiio.awready <> awready
      axiio.axiio.wid <> wid
      axiio.axiio.wdata <> wdata
      axiio.axiio.wstrb <> wstrb
      axiio.axiio.wlast <> wlast
      axiio.axiio.wvalid <> wvalid
      axiio.axiio.wready <> wready
      axiio.axiio.bid <> bid
      axiio.axiio.bresp <> bresp
      axiio.axiio.bvalid <> bvalid
      axiio.axiio.bready <> bready
    }

    // reference
    debug_wb_pc <> stageWB.io.memio.pcWB
    debug_wb_rf_wen <> Cat(Seq.fill(4)(stageWB.io.regfileio.wen))
    debug_wb_rf_wnum <> stageWB.io.regfileio.waddr
    debug_wb_rf_wdata <> stageWB.io.regfileio.wdata
  }
}

object SysTop extends App {
  implicit val config = new Config

  // 目前可使用的的可配置项只有tlb
//  config.hasTLB = true

  println(getVerilogString(new SysTop()))

  // 使用时注意修改生成verilog文件的路径
  // 注意：verilog文件夹的内容需要手动拷贝至目标路径
  val verilogFilePath = "E:\\A_loong\\func_test_v0.01\\soc_axi_func\\rtl\\myCPU\\"

  (new ChiselStage).emitVerilog(new SysTop(), Array("--target-dir", verilogFilePath))
}
