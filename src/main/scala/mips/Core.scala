package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import chisel3.internal.plugin.autoNameRecursively
import config.Config._
import config._

class CoreIO extends Bundle {
  val cacheIF = new IF2Cache_IO
  val cacheID = new ID2Cache_IO
  val cacheMEM = new MEM2Cache_IO
  val cacheWB = new WB2Cache_IO

  val int = inputUInt(int_width)
  val referencePC = outputUInt(machine_width)
  val referenceWEN = outputUInt(4)
  val referenceWNUM = outputUInt(regAddr_width)
  val referenceWDATA = outputUInt(machine_width)
}

/*
  !!! 已作废
 */
class Core(implicit config: Config) extends Module {
  val io = IO(new CoreIO)

  // 模块 module
  val stageIF = Module(new StageIF())
  val stageID = Module(new StageID())
  val stageEXE = Module(new StageEXE())
  val stageMEM = Module(new StageMEM())
  val stageWB = Module(new StageWB())
  val cp0 = Module(new CP0())
  val regFile = Module(new RegFile)
  val scu = Module(new SCU)
  val tlb = if(config.hasTLB) Some(Module(new TLB())) else None

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
  cp0.io.int <> io.int
  cp0.io.flushio <> stageIF.io.flushio
  cp0.io.flushio <> stageID.io.flushio
  cp0.io.flushio <> stageEXE.io.flushio
  cp0.io.flushio <> stageMEM.io.flushio
  cp0.io.flushio.flush <> stageWB.io.flush

  // cache
  stageIF.io.cacheio <> io.cacheIF
  stageID.io.cacheio <> io.cacheID
  stageMEM.io.cacheio <> io.cacheMEM
  stageWB.io.cacheio <> io.cacheWB

  // scu todo 修改
  stageIF.io.scuio.stallReq <> scu.io.stallReqIF
  stageIF.io.scuio.stall <> scu.io.stall
  stageID.io.scuio.stallReq <> scu.io.stallReqID
  stageID.io.scuio.stall <> scu.io.stall
  stageEXE.io.scuio.stallReq <> scu.io.stallReqEXE
  stageEXE.io.scuio.stall <> scu.io.stall
  stageMEM.io.scuio.stallReq <> scu.io.stallReqMEM
  stageMEM.io.scuio.stall <> scu.io.stall
  stageWB.io.stall <> scu.io.stall

  // reference
  io.referencePC <> stageWB.io.memio.pcWB
  io.referenceWEN <> Cat(Seq.fill(4)(stageWB.io.regfileio.wen))
  io.referenceWNUM <> stageWB.io.regfileio.waddr
  io.referenceWDATA <> stageWB.io.regfileio.wdata
}

object Core extends App {
  implicit val config = new Config
  println(getVerilogString(new Core()))
//   (new ChiselStage).emitVerilog(new Core(), Array("--target-dir", "output/"))
}
