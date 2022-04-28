package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

class Crossbar2ICache_IO extends Bundle {
  val req      = inputBool
  val uncached = inputBool
  val addr     = inputUInt(machine_width)
  val addrOk   = outputBool
  val beatOk   = outputBool
  val dataOk   = outputBool
  val rdata    = outputUInt(machine_width)
}

class Crossbar2DCache_IO(implicit config: Config) extends Bundle {
  val rreq      = inputBool
  val wreq      = inputBool
  val we        = inputUInt(4)
  val runcached = inputBool
  val wuncached = inputBool
  val raddr     = inputUInt(machine_width)
  val waddr     = inputUInt(machine_width)
  val wdata     = inputUInt(machine_width * Math.pow(2, config.dcacheConfig.offset_width - 2).toInt)
  val waddrOk   = outputBool
  val raddrOk   = outputBool
  val beatOk    = outputBool
  val dataOk    = outputBool
  val rdata     = outputUInt(machine_width)
}

class AXIMaster_IO extends Bundle {
  val arid    = outputUInt(4)
  val araddr  = outputUInt(machine_width)
  val arlen   = outputUInt(8)
  val arsize  = outputUInt(3)
  val arburst = outputUInt(2)
  val arlock  = outputUInt(2)
  val arcache = outputUInt(4)
  val arprot  = outputUInt(3)
  val arvalid = outputBool
  val arready = inputBool

  val rid     = inputUInt(4)
  val rdata   = inputUInt(machine_width)
  val rresp   = inputUInt(2)
  val rlast   = inputBool
  val rvalid  = inputBool
  val rready  = outputBool
  val awid    = outputUInt(4)
  val awaddr  = outputUInt(machine_width)
  val awlen   = outputUInt(8)
  val awsize  = outputUInt(3)
  val awburst = outputUInt(2)
  val awlock  = outputUInt(2)
  val awcache = outputUInt(4)
  val awprot  = outputUInt(3)
  val awvalid = outputBool
  val awready = inputBool

  val wid    = outputUInt(4)
  val wdata  = outputUInt(machine_width)
  val wstrb  = outputUInt(4)
  val wlast  = outputBool
  val wvalid = outputBool
  val wready = inputBool

  val bid    = inputUInt(4)
  val bresp  = inputUInt(2)
  val bvalid = inputBool
  val bready = outputBool
}

class SRAM2AXIIO(implicit config: Config) extends Bundle {
  val icacheio = new Crossbar2ICache_IO()
  val dcacheio = new Crossbar2DCache_IO()
  val axiio    = new AXIMaster_IO
}

class SRAM2AXI(implicit config: Config) extends Module {
  val io       = IO(new SRAM2AXIIO)
  val icacheio = io.icacheio
  val dcacheio = io.dcacheio
  val axiio    = io.axiio

  class Crossbar extends BlackBox {
    override def desiredName = "sram_to_axi"

    val io = IO(new Bundle() {
      val resetn = inputBool
      val clk    = Input(Clock())

      val inst_req       = inputBool
      val inst_uncached  = inputBool
      val inst_addr      = inputUInt(machine_width)
      val inst_addr_ok   = outputBool
      val inst_beat_ok   = outputBool
      val inst_data_ok   = outputBool
      val inst_rdata     = outputUInt(machine_width)
      val data_rreq      = inputBool
      val data_wreq      = inputBool
      val data_we        = inputUInt(4)
      val data_runcached = inputBool
      val data_wuncached = inputBool
      val data_raddr     = inputUInt(machine_width)
      val data_waddr     = inputUInt(machine_width)
      val data_rdata     = outputUInt(machine_width)
      val data_wdata     = inputUInt(machine_width * Math.pow(2, config.dcacheConfig.offset_width - 2).toInt)
      val data_raddr_ok  = outputBool
      val data_waddr_ok  = outputBool
      val data_beat_ok   = outputBool
      val data_data_ok   = outputBool

      val arid    = outputUInt(4)
      val araddr  = outputUInt(machine_width)
      val arlen   = outputUInt(8)
      val arsize  = outputUInt(3)
      val arburst = outputUInt(2)
      val arlock  = outputUInt(2)
      val arcache = outputUInt(4)
      val arprot  = outputUInt(3)
      val arvalid = outputBool
      val arready = inputBool

      val rid     = inputUInt(4)
      val rdata   = inputUInt(machine_width)
      val rresp   = inputUInt(2)
      val rlast   = inputBool
      val rvalid  = inputBool
      val rready  = outputBool
      val awid    = outputUInt(4)
      val awaddr  = outputUInt(machine_width)
      val awlen   = outputUInt(8)
      val awsize  = outputUInt(3)
      val awburst = outputUInt(2)
      val awlock  = outputUInt(2)
      val awcache = outputUInt(4)
      val awprot  = outputUInt(3)
      val awvalid = outputBool
      val awready = inputBool

      val wid    = outputUInt(4)
      val wdata  = outputUInt(machine_width)
      val wstrb  = outputUInt(4)
      val wlast  = outputBool
      val wvalid = outputBool
      val wready = inputBool

      val bid    = inputUInt(4)
      val bresp  = inputUInt(2)
      val bvalid = inputBool
      val bready = outputBool
    })
  }

  val crossbar   = Module(new Crossbar)
  val crossbario = crossbar.io

  crossbario.resetn := !reset.asBool
  crossbario.clk <> clock

  crossbario.inst_req <> icacheio.req
  crossbario.inst_uncached <> icacheio.uncached
  crossbario.inst_addr <> icacheio.addr
  crossbario.inst_addr_ok <> icacheio.addrOk
  crossbario.inst_beat_ok <> icacheio.beatOk
  crossbario.inst_data_ok <> icacheio.dataOk
  crossbario.inst_rdata <> icacheio.rdata

  crossbario.data_rreq <> dcacheio.rreq
  crossbario.data_wreq <> dcacheio.wreq
  crossbario.data_we <> dcacheio.we
  crossbario.data_runcached <> dcacheio.runcached
  crossbario.data_wuncached <> dcacheio.wuncached
  crossbario.data_raddr <> dcacheio.raddr
  crossbario.data_waddr <> dcacheio.waddr
  crossbario.data_rdata <> dcacheio.rdata
  crossbario.data_wdata <> dcacheio.wdata
  crossbario.data_raddr_ok <> dcacheio.raddrOk
  crossbario.data_waddr_ok <> dcacheio.waddrOk
  crossbario.data_beat_ok <> dcacheio.beatOk
  crossbario.data_data_ok <> dcacheio.dataOk

  crossbario.arid <> axiio.arid
  crossbario.araddr <> axiio.araddr
  crossbario.arlen <> axiio.arlen
  crossbario.arsize <> axiio.arsize
  crossbario.arburst <> axiio.arburst
  crossbario.arlock <> axiio.arlock
  crossbario.arcache <> axiio.arcache
  crossbario.arprot <> axiio.arprot
  crossbario.arvalid <> axiio.arvalid
  crossbario.arready <> axiio.arready
  crossbario.rid <> axiio.rid
  crossbario.rdata <> axiio.rdata
  crossbario.rresp <> axiio.rresp
  crossbario.rlast <> axiio.rlast
  crossbario.rvalid <> axiio.rvalid
  crossbario.rready <> axiio.rready
  crossbario.awid <> axiio.awid
  crossbario.awaddr <> axiio.awaddr
  crossbario.awlen <> axiio.awlen
  crossbario.awsize <> axiio.awsize
  crossbario.awburst <> axiio.awburst
  crossbario.awlock <> axiio.awlock
  crossbario.awcache <> axiio.awcache
  crossbario.awprot <> axiio.awprot
  crossbario.awvalid <> axiio.awvalid
  crossbario.awready <> axiio.awready
  crossbario.wid <> axiio.wid
  crossbario.wdata <> axiio.wdata
  crossbario.wstrb <> axiio.wstrb
  crossbario.wlast <> axiio.wlast
  crossbario.wvalid <> axiio.wvalid
  crossbario.wready <> axiio.wready
  crossbario.bid <> axiio.bid
  crossbario.bresp <> axiio.bresp
  crossbario.bvalid <> axiio.bvalid
  crossbario.bready <> axiio.bready

}


//object SRAM2AXI extends App {
//  implicit val config = new Config
//  println(getVerilogString(new SRAM2AXI()))
//  // (new ChiselStage).emitVerilog(new SRAM2AXI(), Array("--target-dir", "output/"))
//}
