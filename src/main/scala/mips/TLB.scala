package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

/*
  tlb 需要解决的问题及方案：
  1、mtc0指令写tlb相关寄存器，而tlbp指令、取指、访存均需要读取entryhi寄存器，存在相关
  译码出mtc0指令且写entryhi则标记之后的指令重取，重取的指令不能发起访存请求、不能写hilo寄存器
  会引发重取的指令：mtc0写entryhi，tlbr，tlbwi
  重取：由于引发重取的指令没有跳转功能，所以pc可由pc+4得到
  2、tlb例外和分支延迟槽指令
  3、tlbp在访存、tlbr在写回，会出现先相关，所以要设计优先级，优先处理写回级的事务，访存级靠重取解决相关。
  4、tlbp复用mfc0的读通路读vpn2、mtc0的写通路写index,其前推可以解决两条相邻tlbp的相关性，虽然可能实际上不会出现这种相关
  5、tlb指令出现频率很低，相应的设计周期多点没关系
 */

// 常用数据类型封装 useful data type encapsulation
class VPN2_ASID_Bundle extends Bundle {
  val vpn2 = UInt(tlb_vpn2_width.W)
  val asid = UInt(tlb_asid_width.W)
}

class PFN_CDV_Bundle extends Bundle {
  val pfn = UInt(tlb_pfn_width.W)
  val c   = UInt(tlb_c_width.W)
  val d   = UInt(tlb_d_width.W)
  val v   = UInt(tlb_v_width.W)
}

// IO端口定义 IO port definition
class TLB2IF_IO(implicit config: Config) extends Bundle {
  val vpn2    = inputUInt(tlb_vpn2_width)
  val odd     = inputUInt(tlb_odd_width)
  val found   = outputBool
  val pfn_cdv = Output(new PFN_CDV_Bundle)
}

class TLB2MEM_IO(implicit config: Config) extends TLB2IF_IO(){
  val index   = outputUInt(config.tlb_index_width)
}

class TLB2WB_IO() extends Bundle {
  val we = inputBool
}

class TLB2CP0_IO(implicit config: Config) extends Bundle {
  val cp02tlb = Input(new Bundle() {
    val index     = UInt(config.tlb_index_width.W)
    val vpn2_asid = new VPN2_ASID_Bundle
    val pfn_cdv0  = new PFN_CDV_Bundle
    val pfn_cdv1  = new PFN_CDV_Bundle
    val g         = UInt(tlb_g_width.W)
  })

  val tlb2cp0 = Output(new Bundle() {
    val vpn2_asid = new VPN2_ASID_Bundle
    val pfn_cdv0  = new PFN_CDV_Bundle
    val pfn_cdv1  = new PFN_CDV_Bundle
    val g         = UInt(tlb_g_width.W)
  })
}

class TLBIO(implicit config: Config) extends Bundle {
  val ifio  = new TLB2IF_IO()
  val memio = new TLB2MEM_IO()
  val wbio  = new TLB2WB_IO
  val cp0io = new TLB2CP0_IO()
}

/**
 * TLB有两个查找端口，分别对应if和mem阶段的查找。
 * tlbp复用mem端口的查找逻辑，在mem阶段执行，写操作在wb阶段执行
 * tlbr、tlbwi在写回阶段执行
 *
 *
 *
 * @param config
 */
class TLB(implicit config: Config) extends Module {
  val indexWidth = config.tlb_index_width
  val numLine    = math.pow(2, indexWidth).toInt

  // IO
  val io = IO(new TLBIO())

  val tlbio = io

  val ifio       = tlbio.ifio
  val ifioPfnCdv = ifio.pfn_cdv

  val memio       = tlbio.memio
  val memioPfnCdv = memio.pfn_cdv

  val cp0ioIn  = tlbio.cp0io.cp02tlb
  val cp0ioOut = tlbio.cp0io.tlb2cp0

  /*
     寄存器堆 reg file
     +---------------------------------------+
     |VPN2|ASID|G|PFN0|C0|D0|V0|PFN1|C1|D1|V1|
     +---------------------------------------+
  */

  val regBundle = new Bundle() {
    val vpn2_asid = new VPN2_ASID_Bundle
    val g         = UInt(tlb_g_width.W)
    val pfn_cdv0  = new PFN_CDV_Bundle
    val pfn_cdv1  = new PFN_CDV_Bundle
  }
  val reg       = Reg(Vec(numLine, regBundle))

  // 查找逻辑 search logic
  val ifMatch   = Wire(Vec(numLine, Bool()))
  val memMatch  = Wire(Vec(numLine, Bool()))
  val searchMap = Map(ifMatch -> ifio, memMatch -> memio)
  // 默认输出 default output
  Seq(ifioPfnCdv, memioPfnCdv).foreach {
    case io => {
      io := reg(0).pfn_cdv0
    }
  }

  searchMap.foreach {
    case (m, io) => m.zipWithIndex.foreach {
      case (x, i) => {
        val condition = (io.vpn2 === reg(i).vpn2_asid.vpn2 &&
          (cp0ioIn.vpn2_asid.asid === reg(i).vpn2_asid.asid || reg(i).g.asBool))
        x := condition
        when(io.odd.asBool && condition) {
          io.pfn_cdv := reg(i).pfn_cdv1
        }.elsewhen(!io.odd && condition) {
          io.pfn_cdv := reg(i).pfn_cdv0
        }
      }
    }
  }

  searchMap.foreach {
    case (m, io) => {
      io.found := m.reduce(_ | _)
    }
  }
  memio.index := memMatch.indexWhere(_ === true.B)

  // 写入逻辑 write logic
  when(io.wbio.we) {
    val wReg = reg(cp0ioIn.index)
    wReg.vpn2_asid := cp0ioIn.vpn2_asid
    wReg.g := cp0ioIn.g
    wReg.pfn_cdv0 := cp0ioIn.pfn_cdv0
    wReg.pfn_cdv1 := cp0ioIn.pfn_cdv1
  }

  // 读取逻辑 read logic
  val rReg = reg(cp0ioIn.index)

  cp0ioOut.vpn2_asid := rReg.vpn2_asid
  cp0ioOut.g := rReg.g
  cp0ioOut.pfn_cdv0 := rReg.pfn_cdv0
  cp0ioOut.pfn_cdv1 := rReg.pfn_cdv1
}

object TLB extends App {
  implicit val config = new Config
  println(getVerilogString(new TLB()))
  // (new ChiselStage).emitVerilog(new TLB(), Array("--target-dir", "output/"))
}
