package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

class ICacheIO(implicit config: Config) extends Bundle {
  val ifio       = Flipped(new IF2Cache_IO)
  val idio       = Flipped(new ID2Cache_IO)
  val mmuio      = Flipped(new MMU2Cache_IO)
  val crossbario = Flipped(new Crossbar2ICache_IO)
}

class ICache(implicit config: Config) extends Module {
  val io         = IO(new ICacheIO)
  val ifio       = io.ifio
  val idio       = io.idio
  val mmuio      = io.mmuio
  val crossbario = io.crossbario

  class ICacheVerilog extends BlackBox {
    override def desiredName = "inst_cache"

    val io = IO(new Bundle() {
      val rst = inputBool
      val clk = Input(Clock())

      val cpu_req          = inputBool
      val cpu_addr         = inputUInt(machine_width)
      val cpu_uncached     = inputBool
      val cpu_addr_ok      = outputBool
      val cpu_operation_ok = outputBool
      val cpu_rdata        = outputUInt(machine_width)
      val ram_req          = outputUInt(4)
      val ram_uncached     = outputBool
      val ram_addr         = outputUInt(machine_width)
      val ram_addr_ok      = inputBool
      val ram_beat_ok      = inputBool
      val ram_data_ok      = inputBool
      val ram_rdata        = inputUInt(machine_width)
    })
  }

  val cache   = Module(new ICacheVerilog)
  val cacheio = cache.io

  cacheio.clk <> clock
  cacheio.rst := !reset.asBool
  cacheio.cpu_req <> ifio.instReq
  cacheio.cpu_addr <> mmuio.addr
  cacheio.cpu_uncached <> mmuio.uncached
  cacheio.cpu_addr_ok <> ifio.instAddrOK
  cacheio.cpu_operation_ok <> ifio.dataOK
  cacheio.cpu_rdata <> idio.inst
  cacheio.ram_req <> crossbario.req
  cacheio.ram_uncached <> crossbario.uncached
  cacheio.ram_addr <> crossbario.addr
  cacheio.ram_addr_ok <> crossbario.addrOk
  cacheio.ram_beat_ok <> crossbario.beatOk
  cacheio.ram_data_ok <> crossbario.dataOk
  cacheio.ram_rdata <> crossbario.rdata
}

object ICache extends App {
  implicit val config = new Config
  println(getVerilogString(new ICache()))
  // (new ChiselStage).emitVerilog(new ICache(), Array("--target-dir", "output/"))
}
