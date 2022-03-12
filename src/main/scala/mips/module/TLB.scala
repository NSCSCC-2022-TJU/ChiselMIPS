package mips.module

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import Config._

class TLB_PFN_CDV_IO_Out() extends Bundle {
  val pfn = outputUInt(tlb_pfn_width)
  val c   = outputUInt(tlb_c_width)
  val d   = outputUInt(tlb_d_width)
  val v   = outputUInt(tlb_v_width)
}

class TLBSearchIO(indexWidth: Int) extends Bundle {
  val vpn2    = inputUInt(tlb_vpn2_width)
  val odd     = inputUInt(tlb_odd_width)
  val asid    = inputUInt(tlb_asid_width)
  val found   = outputUInt(tlb_found_width)
  val index   = outputUInt(indexWidth)
  val pfn_cdv = new TLB_PFN_CDV_IO_Out
}

class TLBWriteIO(indexWidth: Int) extends Bundle {
  val we       = inputBool
  val index    = inputUInt(indexWidth)
  val vpn2     = inputUInt(tlb_vpn2_width)
  val asid     = inputUInt(tlb_asid_width)
  val g        = inputUInt(tlb_g_width)
  val pfn_cdv0 = Flipped(new TLB_PFN_CDV_IO_Out)
  val pfn_cdv1 = Flipped(new TLB_PFN_CDV_IO_Out)
}

class TLBReadIO(indexWidth: Int) extends Bundle {
  val index    = inputUInt(indexWidth)
  val vpn2     = outputUInt(tlb_vpn2_width)
  val asid     = outputUInt(tlb_asid_width)
  val g        = outputUInt(tlb_g_width)
  val pfn_cdv0 = new TLB_PFN_CDV_IO_Out
  val pfn_cdv1 = new TLB_PFN_CDV_IO_Out
}

class TLBIO(indexWidth: Int) extends Bundle {

  // 取指端口 IF port
  val ifIO    = new TLBSearchIO(indexWidth)
  // 访存端口 MEM port
  val memIO   = new TLBSearchIO(indexWidth)
  // 写端口 write port
  val writeIO = new TLBWriteIO(indexWidth)
  // 读端口 read port
  val readIO  = new TLBReadIO(indexWidth)
}

class TLB(implicit config: Config) extends Module {
  val indexWidth = config.tlb_index_width
  val numLine    = math.pow(2, indexWidth).toInt

  // IO
  val io = IO(new TLBIO(indexWidth))

  val tlbio = io

  val ifio       = tlbio.ifIO
  val ifioPfnCdv = ifio.pfn_cdv

  val memio       = tlbio.memIO
  val memioPfnCdv = memio.pfn_cdv

  val writeio        = tlbio.writeIO
  val writeioPfnCdv0 = writeio.pfn_cdv0
  val writeioPfnCdv1 = writeio.pfn_cdv1

  val readio        = tlbio.readIO
  val readioPfnCdv0 = readio.pfn_cdv0
  val readioPfnCdv1 = readio.pfn_cdv1

  /*
     寄存器堆 reg file
     +---------------------------------------+
     |VPN2|ASID|G|PFN0|C0|D0|V0|PFN1|C1|D1|V1|
     +---------------------------------------+
  */
  val regBundle = new Bundle() {
    val vpn2 = UInt(tlb_vpn2_width.W)
    val asid = UInt(tlb_asid_width.W)
    val g    = UInt(tlb_g_width.W)
    val pfn0 = UInt(tlb_pfn_width.W)
    val c0   = UInt(tlb_c_width.W)
    val d0   = UInt(tlb_d_width.W)
    val v0   = UInt(tlb_v_width.W)
    val pfn1 = UInt(tlb_pfn_width.W)
    val c1   = UInt(tlb_c_width.W)
    val d1   = UInt(tlb_d_width.W)
    val v1   = UInt(tlb_v_width.W)
  }
  val reg       = Reg(Vec(numLine, regBundle))

  // 查找 search
  val ifMatch   = Wire(Vec(numLine, Bool()))
  val memMatch  = Wire(Vec(numLine, Bool()))
  val searchMap = Map(ifMatch -> ifio, memMatch -> memio)
  // 默认输出 default output
  Seq(ifio, memio).foreach {
    case io => {
      io.pfn_cdv.pfn := reg(0).pfn1
      io.pfn_cdv.c := reg(0).c1
      io.pfn_cdv.d := reg(0).d1
      io.pfn_cdv.v := reg(0).v1
    }
  }

  searchMap.foreach {
    case (m, io) => m.zipWithIndex.foreach {
      case (x, i) => {
        val condition = (io.vpn2 === reg(i).vpn2 && (io.asid === reg(i).asid || reg(i).g.asBool))
        x := condition
        when(io.odd.asBool && condition) {
          io.pfn_cdv.pfn := reg(i).pfn1
          io.pfn_cdv.c := reg(i).c1
          io.pfn_cdv.d := reg(i).d1
          io.pfn_cdv.v := reg(i).v1
        }.elsewhen(!io.odd && condition) {
          io.pfn_cdv.pfn := reg(i).pfn0
          io.pfn_cdv.c := reg(i).c0
          io.pfn_cdv.d := reg(i).d0
          io.pfn_cdv.v := reg(i).v0
        }
      }
    }
  }

  searchMap.foreach {
    case (m, io) => {
      io.found := m.reduce(_ | _)
      io.index := m.indexWhere(_ === true.B)
    }
  }

  // 写入 write
  when(writeio.we) {
    val wReg = reg(writeio.index)
    wReg.vpn2 := writeio.vpn2
    wReg.asid := writeio.asid
    wReg.g := writeio.g
    wReg.pfn0 := writeioPfnCdv0.pfn
    wReg.c0 := writeioPfnCdv0.c
    wReg.d0 := writeioPfnCdv0.d
    wReg.v0 := writeioPfnCdv0.v
    wReg.pfn1 := writeioPfnCdv1.pfn
    wReg.c1 := writeioPfnCdv1.c
    wReg.d1 := writeioPfnCdv1.d
    wReg.v1 := writeioPfnCdv1.v
  }

  // 读取 read
  val rReg = reg(readio.index)
  readio.vpn2 := rReg.vpn2
  readio.asid := rReg.asid
  readio.g := rReg.g
  readioPfnCdv0.pfn := rReg.pfn0
  readioPfnCdv0.c := rReg.c0
  readioPfnCdv0.d := rReg.d0
  readioPfnCdv0.v := rReg.v0
  readioPfnCdv1.pfn := rReg.pfn1
  readioPfnCdv1.c := rReg.c1
  readioPfnCdv1.d := rReg.d1
  readioPfnCdv1.v := rReg.v1
}

object TLB extends App {
  implicit val config = new Config
  println(getVerilogString(new TLB()))
  // (new ChiselStage).emitVerilog(new TLB(), Array("--target-dir", "output/"))
}
