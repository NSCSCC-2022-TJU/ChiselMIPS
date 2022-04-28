package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

class DCacheIO(implicit config: Config) extends Bundle {
  val memio      = Flipped(new MEM2Cache_IO)
  val wbio       = Flipped(new WB2Cache_IO)
  val mmuio      = Flipped(new MMU2Cache_IO)
  val crossbario = Flipped(new Crossbar2DCache_IO)
}

class DCache(implicit config: Config) extends Module {
  val io         = IO(new DCacheIO)
  val memio      = io.memio
  val wbio       = io.wbio
  val mmuio      = io.mmuio
  val crossbario = io.crossbario

  class DCacheVerilog extends BlackBox {
    override def desiredName = "data_cache"

    val io = IO(new Bundle() {
      val rst = inputBool
      val clk = Input(Clock())

      val cpu_req          = inputBool
      val cpu_wre          = inputUInt(4)
      val cpu_wr           = inputBool
      val cpu_addr         = inputUInt(machine_width)
      val cpu_wdata        = inputUInt(machine_width)
      val cpu_uncached     = inputBool
      val cpu_addr_ok      = outputBool
      val cpu_operation_ok = outputBool
      val cpu_rdata        = outputUInt(machine_width)
      val ram_rreq         = outputBool
      val ram_wreq         = outputBool
      val ram_we           = outputUInt(4)
      val ram_runcached    = outputBool
      val ram_wuncached    = outputBool
      val ram_raddr        = outputUInt(machine_width)
      val ram_waddr        = outputUInt(machine_width)
      val ram_wdata        = outputUInt(machine_width * Math.pow(2, config.dcacheConfig.offset_width - 2).toInt)
      val ram_waddr_ok     = inputBool
      val ram_raddr_ok     = inputBool
      val ram_beat_ok      = inputBool
      val ram_data_ok      = inputBool
      val ram_rdata        = inputUInt(machine_width)
    })
  }

  val cache = Module(new DCacheVerilog)
  val cacheio = cache.io

  cacheio.clk <> clock
  cacheio.rst := !reset.asBool
  cacheio.cpu_req <> memio.req
  cacheio.cpu_wre := Mux(memio.we === 0.U,"b1111".U(4.W),memio.we)
  cacheio.cpu_wr := memio.we =/= 0.U
  cacheio.cpu_addr := Cat(mmuio.addr(31,2),0.U(2.W))
  cacheio.cpu_wdata <> memio.din
  cacheio.cpu_uncached <> mmuio.uncached
//  cacheio.cpu_addr_ok <> 该端口可以删去
  cacheio.cpu_operation_ok <> memio.dataOk
  cacheio.cpu_operation_ok <> wbio.dataOk
  cacheio.cpu_rdata <> wbio.rdata
  cacheio.ram_rreq <> crossbario.rreq
  cacheio.ram_wreq <> crossbario.wreq
  cacheio.ram_we <> crossbario.we
  cacheio.ram_runcached <> crossbario.runcached
  cacheio.ram_wuncached <> crossbario.wuncached
  cacheio.ram_raddr <> crossbario.raddr
  cacheio.ram_waddr <> crossbario.waddr
  cacheio.ram_wdata <> crossbario.wdata
  cacheio.ram_waddr_ok <> crossbario.waddrOk
  cacheio.ram_raddr_ok <> crossbario.raddrOk
  cacheio.ram_beat_ok <> crossbario.beatOk
  cacheio.ram_data_ok <> crossbario.dataOk
  cacheio.ram_rdata <> crossbario.rdata
}

object DCache extends App {
  implicit val config = new Config
  println(getVerilogString(new DCache()))
  // (new ChiselStage).emitVerilog(new DCache(), Array("--target-dir", "output/"))
}
