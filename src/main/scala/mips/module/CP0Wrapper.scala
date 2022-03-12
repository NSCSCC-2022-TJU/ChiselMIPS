package mips.module

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import Config._

/**
 * 面向原有verilog文件的cp0封装 A cp0 wrapper for old verilog files
 */
class CP0Wrapper(implicit config: Config) extends RawModule {

  override def desiredName = "cp0_reg" //指定生成的模块名 specify the name of this module

  val cpu_clk_50M = IO(Input(Clock()))
  val cpu_rst_n   = IO(inputBool)
  val we          = IO(inputBool)
  val raddr       = IO(inputUInt(cp0_regAddr_width))
  val waddr       = IO(inputUInt(cp0_regAddr_width))
  val wdata       = IO(inputUInt(machine_width))
  val int_i       = IO(inputUInt(int_width))
  val pc_i        = IO(inputUInt(machine_width))
  val in_delay_i  = IO(inputBool)
  val exccode_i   = IO(inputUInt(excCode_width))
  val badvaddr_i  = IO(inputUInt(machine_width))
  val flush       = IO(outputBool)
  val cp0_excaddr = IO(outputUInt(machine_width))
  val data_o      = IO(outputUInt(machine_width))
  val status_o    = IO(outputUInt(machine_width))
  val cause_o     = IO(outputUInt(machine_width))

  val found          = IO(inputBool)
  val index_i        = IO(inputUInt(config.tlb_index_width))
  val vpn2_i         = IO(inputUInt(tlb_vpn2_width))
  val asid_i         = IO(inputUInt(tlb_asid_width))
  val pfn_cdvg_width = tlb_pfn_width + tlb_c_width + tlb_d_width + tlb_v_width + tlb_g_width
  val pfn_cdvg0_i    = IO(inputUInt(pfn_cdvg_width))
  val pfn_cdvg1_i    = IO(inputUInt(pfn_cdvg_width))
  val index_o        = IO(outputUInt(config.tlb_index_width))
  val vpn2_o         = IO(outputUInt(tlb_vpn2_width))
  val asid_o         = IO(outputUInt(tlb_asid_width))
  val pfn_cdv_width  = tlb_pfn_width + tlb_c_width + tlb_d_width + tlb_v_width
  val pfn_cdv0_o     = IO(outputUInt(pfn_cdv_width))
  val pfn_cdv1_o     = IO(outputUInt(pfn_cdv_width))
  val g_o            = IO(outputBool)

  withClockAndReset(cpu_clk_50M, !cpu_rst_n) {
    val cp0 = Module(new CP0())

    val cp0io = cp0.io
    val regIO = cp0io.regIO

    we <> cp0io.we
    raddr <> cp0io.raddr
    waddr <> cp0io.waddr
    wdata <> cp0io.wdata
    int_i <> cp0io.int
    pc_i <> cp0io.pc
    in_delay_i <> cp0io.inDelay
    exccode_i <> cp0io.excCode
    badvaddr_i <> cp0io.badVaddr
    flush <> cp0io.flush
    cp0_excaddr <> cp0io.excAddr
    data_o <> cp0io.data
    status_o <> regIO.status
    cause_o <> regIO.cause

    found <> cp0io.tlbFound
    index_i<>cp0io.regIO.tlbIn.index
    vpn2_i<>cp0io.regIO.tlbIn.entryhi.vpn2
    asid_i<>cp0io.regIO.tlbIn.entryhi.asid
    pfn_cdvg0_i<>cp0io.regIO.tlbIn.entrylo0
    pfn_cdvg1_i<>cp0io.regIO.tlbIn.entrylo1
    index_o<>cp0io.
    vpn2_o<>cp0io.
    asid_o<>cp0io.
    pfn_cdv_width<>cp0io.
    pfn_cdv0_o<>cp0io.
    pfn_cdv1_o<>cp0io.
    g_o<>cp0io.
  }
}

object CP0Wrapper extends App {
  implicit val config = new Config
  config.debug = false
  println(getVerilogString(new CP0Wrapper()))
  (new ChiselStage).emitVerilog(new CP0Wrapper(), Array("--target-dir", "output/"))
}
