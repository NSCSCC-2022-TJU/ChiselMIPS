package mips.module

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import Config._

/**
 * 面向龙芯测试文件的TLB封装 A TLB wrapper for Loong test
 */
class TLBWrapper(implicit config:Config) extends RawModule {
  val tlbIndexWidth = config.tlb_index_width
  override def desiredName = "tlb" //指定生成的模块名 specify the name of this module

  val clk = IO(Input(Clock()))

  val s0_vpn2     = IO(inputUInt(tlb_vpn2_width))
  val s0_odd_page = IO(inputBool)
  val s0_asid     = IO(inputUInt(tlb_asid_width))
  val s0_found    = IO(outputBool)
  val s0_index    = IO(outputUInt(tlbIndexWidth))
  val s0_pfn      = IO(outputUInt(tlb_pfn_width))
  val s0_c        = IO(outputUInt(tlb_c_width))
  val s0_d        = IO(outputBool)
  val s0_v        = IO(outputBool)

  val s1_vpn2     = IO(inputUInt(tlb_vpn2_width))
  val s1_odd_page = IO(inputBool)
  val s1_asid     = IO(inputUInt(tlb_asid_width))
  val s1_found    = IO(outputBool)
  val s1_index    = IO(outputUInt(tlbIndexWidth))
  val s1_pfn      = IO(outputUInt(tlb_pfn_width))
  val s1_c        = IO(outputUInt(tlb_c_width))
  val s1_d        = IO(outputBool)
  val s1_v        = IO(outputBool)

  val we      = IO(inputBool)
  val w_index = IO(inputUInt(tlbIndexWidth))
  val w_vpn2  = IO(inputUInt(tlb_vpn2_width))
  val w_asid  = IO(inputUInt(tlb_asid_width))
  val w_g     = IO(inputBool)
  val w_pfn0  = IO(inputUInt(tlb_pfn_width))
  val w_c0    = IO(inputUInt(tlb_c_width))
  val w_d0    = IO(inputBool)
  val w_v0    = IO(inputBool)
  val w_pfn1  = IO(inputUInt(tlb_pfn_width))
  val w_c1    = IO(inputUInt(tlb_c_width))
  val w_d1    = IO(inputBool)
  val w_v1    = IO(inputBool)

  val r_index = IO(inputUInt(tlbIndexWidth))
  val r_vpn2  = IO(outputUInt(tlb_vpn2_width))
  val r_asid  = IO(outputUInt(tlb_asid_width))
  val r_g     = IO(outputBool)
  val r_pfn0  = IO(outputUInt(tlb_pfn_width))
  val r_c0    = IO(outputUInt(tlb_c_width))
  val r_d0    = IO(outputBool)
  val r_v0    = IO(outputBool)
  val r_pfn1  = IO(outputUInt(tlb_pfn_width))
  val r_c1    = IO(outputUInt(tlb_c_width))
  val r_d1    = IO(outputBool)
  val r_v1    = IO(outputBool)

  withClockAndReset(clk, 0.B) {
    val tlb   = Module(new TLB())
    val tlbio = tlb.io

    val ifio       = tlbio.ifIO
    val ifioPfnCdv = ifio.pfn_cdv

    val memio     = tlbio.memIO
    val memioPfnCdv = memio.pfn_cdv

    val writeio      = tlbio.writeIO
    val writeioPfnCdv0 = writeio.pfn_cdv0
    val writeioPfnCdv1 = writeio.pfn_cdv1

    val readio      = tlbio.readIO
    val readioPfnCdv0 = readio.pfn_cdv0
    val readioPfnCdv1 = readio.pfn_cdv1


    s0_vpn2 <> ifio.vpn2
    s0_odd_page <> ifio.odd
    s0_asid <> ifio.asid
    s0_found <> ifio.found
    s0_index <> ifio.index
    s0_pfn <> ifioPfnCdv.pfn
    s0_c <> ifioPfnCdv.c
    s0_d <> ifioPfnCdv.d
    s0_v <> ifioPfnCdv.v
    s1_vpn2 <> memio.vpn2
    s1_odd_page <> memio.odd
    s1_asid <> memio.asid
    s1_found <> memio.found
    s1_index <> memio.index
    s1_pfn <> memioPfnCdv.pfn
    s1_c <> memioPfnCdv.c
    s1_d <> memioPfnCdv.d
    s1_v <> memioPfnCdv.v
    we <> writeio.we
    w_index <> writeio.index
    w_vpn2 <> writeio.vpn2
    w_asid <> writeio.asid
    w_g <> writeio.g
    w_pfn0 <> writeioPfnCdv0.pfn
    w_c0 <> writeioPfnCdv0.c
    w_d0 <> writeioPfnCdv0.d
    w_v0 <> writeioPfnCdv0.v
    w_pfn1 <> writeioPfnCdv1.pfn
    w_c1 <> writeioPfnCdv1.c
    w_d1 <> writeioPfnCdv1.d
    w_v1 <> writeioPfnCdv1.v
    r_index <> readio.index
    r_vpn2 <> readio.vpn2
    r_asid <> readio.asid
    r_g <> readio.g
    r_pfn0 <> readioPfnCdv0.pfn
    r_c0 <> readioPfnCdv0.c
    r_d0 <> readioPfnCdv0.d
    r_v0 <> readioPfnCdv0.v
    r_pfn1 <> readioPfnCdv1.pfn
    r_c1 <> readioPfnCdv1.c
    r_d1 <> readioPfnCdv1.d
    r_v1 <> readioPfnCdv1.v
  }
}

object TLBWrapper extends App {
  implicit val config = new Config
//  println(getVerilogString(new TLBWrapper(new Config)))
   (new ChiselStage).emitVerilog(new TLBWrapper(), Array("--target-dir", "output/"))
}
