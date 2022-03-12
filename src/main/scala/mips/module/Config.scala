package mips.module

import chisel3._
import chisel3.experimental.ChiselEnum

/**
 * 可变配置 variable configuration
 * eg: cache lines, TLB index width
 */
class Config {
  var debug           = true
  var statistics      = false
  /**
   * default: 4
   */
  var tlb_index_width = 4
}

/**
 * 不可变配置 fixed configuration
 * eg: instruction width
 */
object Config {
  // 全局参数 global parameter
  /**
   * 32
   */
  val machine_width = 32
  /**
   * 6
   */
  val int_width     = 6
  /**
   * 5
   */
  val excCode_width = 5

  val excInt   = "h0".U
  val excSys   = "h8".U
  val excOv    = "hc".U
  val excNone  = "h10".U
  val excEret  = "h11".U
  val excAdel  = "h04".U
  val excRi    = "h0a".U
  val excAdes  = "h05".U
  val excBreak = "h09".U

  val excAddr    = "hbfc00380".U
  val excIntAddr = "hbfc00380".U

  // cp0参数 cp0 parameter
  /**
   * 5
   */
  val cp0_regAddr_width = 5
  /**
   * 8
   */
  val cp0_badVaddr_addr = 8.U
  /**
   * 9
   */
  val cp0_count_addr    = 9.U
  /**
   * 12
   */
  val cp0_status_addr   = 12.U
  /**
   * 13
   */
  val cp0_cause_addr    = 13.U
  /**
   * 14
   */
  val cp0_epc_addr      = 14.U
  /**
   * 0
   */
  val cp0_index_addr    = 0.U
  /**
   * 10
   */
  val cp0_entryhi_addr  = 10.U
  /**
   * 2
   */
  val cp0_entrylo0_addr = 2.U
  /**
   * 3
   */
  val cp0_entrylo1_addr = 3.U

  // tlb参数 tlb parameter
  /**
   * 19
   */
  val tlb_vpn2_width  = 19
  /**
   * 1
   */
  val tlb_odd_width   = 1
  /**
   * 8
   */
  val tlb_asid_width  = 8
  /**
   * 20
   */
  val tlb_pfn_width   = 20
  /**
   * 3
   */
  val tlb_c_width     = 3
  /**
   * 1
   */
  val tlb_d_width     = 1
  /**
   * 1
   */
  val tlb_v_width     = 1
  /**
   * 1
   */
  val tlb_g_width     = 1
  /**
   * 1
   */
  val tlb_found_width = 1

  //常用封装 useful encapsulation
  def outputUInt(width: Int) = Output(UInt(width.W))

  def inputUInt(width: Int) = Input(UInt(width.W))

  def outputBool = Output(Bool())

  def inputBool = Input(Bool())

  //  def outputUIntWidth32 = outputUInt(32)
  //
  //  def inputUIntWidth32 = inputUInt(32)
  //
  //  def outputUIntWidth20 = outputUInt(20)
  //
  //  def inputUIntWidth20 = inputUInt(20)
  //
  //  def outputUIntWidth19 = outputUInt(19)
  //
  //  def inputUIntWidth19 = inputUInt(19)
  //
  //  def outputUIntWidth8 = outputUInt(8)
  //
  //  def inputUIntWidth8 = inputUInt(8)
  //
  //  def outputUIntWidth4 = outputUInt(4)
  //
  //  def inputUIntWidth4 = inputUInt(4)
  //
  //  def outputUIntWidth3 = outputUInt(3)
  //
  //  def inputUIntWidth3 = inputUInt(3)

}

//object  extends ChiselEnum {
//
//
//}
