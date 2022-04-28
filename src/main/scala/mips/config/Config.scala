package mips.config

import chisel3._
import chisel3.util.HasBlackBoxInline
import mips.DCUDebugBundle

/**
 * 可变配置 variable configuration
 * eg: cache lines, TLB index width
 */
class Config() {
  val debug         = new DebugConfig
  /*
    debug模式下：
    1、config寄存器的m域为0，关闭为1，指示是否存在config1寄存器
    2、DCU包含一个指令指示器，用来指示DCU中处理的是哪条指令
   */
  var hasStatistics = false // 数据统计开关 statistics switch

  var hasTLB       = false
  var hasCP0Bypass = true // false还没有改好，会出现mtc0写cause寄存器与cause输出相关，导致访存和写回一直暂停
  var hasCache     = true // false不能用
  var hasRegReset  = true // todo 某些寄存器可以不复位

  var pcInit           = "hbfc0_0000".U(32.W) // pc寄存器复位值 pc initial value
  val excAddr          = "hbfc0_0380".U
  val excIntAddr       = "hbfc0_0380".U
  val excTlbRefillAddr = "hbfc0_0200".U

  val icacheConfig = new CacheConfig
  val dcacheConfig = new CacheConfig

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
  // ISA
  /**
   * 32
   */
  val machine_width = 32
  /**
   * 外部中断向量宽度 6
   */
  val int_width     = 6
  /**
   * 异常码宽度 5
   */
  val excCode_width = 5
  /**
   * 寄存器地址宽度 5
   */
  val regAddr_width = 5

  // ExcCode
  val excInt         = "h00".U
  val excMod         = "h01".U
  val excTlbl        = "h02".U
  val excTlbs        = "h03".U
  val excAdel        = "h04".U
  val excAdes        = "h05".U
  val excSys         = "h08".U
  val excBreak       = "h09".U
  val excRi          = "h0a".U
  val excOv          = "h0c".U
  // custom
  // begin
  val excNone        = "h10".U
  val excEret        = "h11".U
  val excTlbRefillL  = "h15".U
  val excTlbRefillS  = "h1b".U
  val excTlbInvalidL = "h1c".U
  val excTlbInvalidS = "h1d".U
  // end

  // cp0参数 cp0 parameter
  /**
   * 0
   */
  val cp0_index_addr    = 0.U
  /**
   * 2
   */
  val cp0_entrylo0_addr = 2.U
  /**
   * 3
   */
  val cp0_entrylo1_addr = 3.U
  /**
   * 8
   */
  val cp0_badVaddr_addr = 8.U
  /**
   * 9
   */
  val cp0_count_addr    = 9.U
  /**
   * 10
   */
  val cp0_entryhi_addr  = 10.U
  /**
   * 11
   */
  val cp0_compare_addr  = 11.U
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
   * 16
   */
  val cp0_config_addr   = 16.U


  // tlb参数 tlb parameter
  /**
   * 19
   */
  val tlb_vpn2_width = 19
  /**
   * 1
   */
  val tlb_odd_width  = 1
  /**
   * 8
   */
  val tlb_asid_width = 8
  /**
   * 20
   */
  val tlb_pfn_width  = 20
  /**
   * 3
   */
  val tlb_c_width    = 3
  /**
   * 1
   */
  val tlb_d_width    = 1
  /**
   * 1
   */
  val tlb_v_width    = 1
  /**
   * 1
   */
  val tlb_g_width    = 1


  // 自定义 custom
  val stall_width = 6


  //常用封装 useful encapsulation
  def outputUInt(width: Int) = Output(UInt(width.W))

  def inputUInt(width: Int) = Input(UInt(width.W))

  def outputBool = Output(Bool())

  def inputBool = Input(Bool())

  def nopWidth32 = 0.U(32.W)

  def wireUInt(width: Int) = Wire(UInt(width.W))
}

class CacheConfig {
  var tag_width    = 20
  var index_width  = 8
  var offset_width = 4
  var association  = 2
  require(index_width >= 5 && index_width <= 12,
    "Cache index width config error: MIPS32 manual(CP0 config1 register) has stipulated cache " +
      "set num per way is at least 32 and at most 4096")
  require(offset_width >= 2 && offset_width <= 7,
    "Cache offset width config error: MIPS32 manual(CP0 config1 register) has stipulated cache " +
      "line size is at least 4B and at most 128B")
  require(association >= 1 && association <= 8,
    "Cache association config error: MIPS32 manual(CP0 config1 register) has stipulated cache" +
      "association is at least 1 and at most 8")
  require(tag_width + index_width + offset_width == 32,
    "Cache address width config error: tag + index + offset != 32")
}

/**
 * 默认全部开启 default: turn on all debug
 */
class DebugConfig {
  var cp0, dcu = true

  def turnOnAll = {
    cp0 = true
    dcu = true
  }

  def turnOffAll = {
    cp0 = false
    dcu = false
  }
}


class DebugInformation extends BlackBox {
  val io = IO(new Bundle() {
    val dcu = Input(new DCUDebugBundle)
  })

}