package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

class MMU2IF_IO(implicit config: Config) extends Bundle {
  val addr   = inputUInt(machine_width)
  val found  = if (config.hasTLB) Some(outputBool) else None
  val v      = if (config.hasTLB) Some(outputBool) else None
  val mapped = if (config.hasTLB) Some(outputBool) else None
}

class MMU2EME_IO(implicit config: Config) extends MMU2IF_IO {
  val index = if (config.hasTLB) Some(outputUInt(config.tlb_index_width)) else None
  val d     = if (config.hasTLB) Some(outputBool) else None
}

class MMU2WB_IO extends TLB2WB_IO

class MMU2Cache_IO extends Bundle {
  val addr     = outputUInt(machine_width)
  val uncached = outputBool
}

class MMU2CP0_IO(implicit config: Config) extends TLB2CP0_IO {
  val k0 = Input((new ConfigRegBundle()).k0.asUInt)
}

class MMUIO(implicit config: Config) extends Bundle {
  val ifio     = new MMU2IF_IO
  val memio    = new MMU2EME_IO
  val wbio     = if (config.hasTLB) Some(new MMU2WB_IO) else None
  val cp0io    = if (config.hasTLB) Some(new MMU2CP0_IO) else None
  val icacheio = new MMU2Cache_IO
  val dcacheio = new MMU2Cache_IO
}

class MMU(implicit config: Config) extends Module {
  /*
    注意：
    1、unmapped段不经过tlb，一定不会引发tlb例外
   */
  val io       = IO(new MMUIO)
  val ifio     = io.ifio
  val memio    = io.memio
  val icacheio = io.icacheio
  val dcacheio = io.dcacheio

  // 统计信息
  //  val inkuseg = if(config.hasStatistics)Some(Wire(Bool()))else None
  //  val ifStatistics,memStatistics = Wire(new Bundle() {
  //    val inkuseg,inkseg0,inkseg1,inkseg2and3,tlbReq,uncachedReq = Bool()
  //  })

  if (config.hasTLB) {
    val wbio     = io.wbio.get
    val cp0io    = io.cp0io.get
    val tlb      = Module(new TLB)
    val tlbio    = tlb.io
    val tlbioMap = Map(ifio -> tlbio.ifio, memio -> tlbio.memio)
    tlbioMap.foreach {
      case (stage, tlb) => {
        tlb.vpn2 := stage.addr(31, 31 - tlb_vpn2_width + 1)
        tlb.odd := stage.addr(31 - tlb_vpn2_width)
        stage.found.get := tlb.found
        stage.v.get := tlb.pfn_cdv.v
      }
    }
    memio.index.get := tlbio.memio.index
    memio.d.get := tlbio.memio.pfn_cdv.d

    val cacheioMap = Map(ifio -> (icacheio, tlbio.ifio), memio -> (dcacheio, tlbio.memio))
    cacheioMap.foreach {
      case (stage, (cache, tlb)) => {
        stage.mapped.get := false.B
        when(stage.addr(31, 28) <= "h7".U) {
          // kuseg : (mapped, tlbc)
          stage.mapped.get := true.B
          cache.uncached := tlb.pfn_cdv.c =/= 3.U
          cache.addr := Cat(tlb.pfn_cdv.pfn, stage.addr(11, 0))
        }.elsewhen(stage.addr(31, 28) <= "h9".U) {
          // kseg0 : (unmapped, config.k0)
          cache.uncached := cp0io.k0 =/= 3.U
          cache.addr := Cat("h0".U(3.W), stage.addr(28, 0))
        }.elsewhen(stage.addr(31, 28) <= "hb".U) {
          // kseg1 : (unmapped, uncached)
          cache.uncached := true.B
          cache.addr := Cat("h0".U(3.W), stage.addr(28, 0))
        }.otherwise {
          // kseg2/3 (mapped, tlbc)
          stage.mapped.get := true.B
          cache.uncached := tlb.pfn_cdv.c =/= 3.U
          cache.addr := Cat(tlb.pfn_cdv.pfn, stage.addr(11, 0))
        }
      }
    }

    wbio <> tlbio.wbio
    cp0io.tlb2cp0 <> tlbio.cp0io.tlb2cp0
    cp0io.cp02tlb <> tlbio.cp0io.cp02tlb

  } else {
    val cacheioMap = Map(ifio -> icacheio, memio -> dcacheio)
    cacheioMap.foreach {
      case (stage, cache) => {
        cache.addr := Mux(
          stage.addr(31, 28) >= "h8".U && stage.addr(31, 28) <= "hb".U,
          Cat("h0".U(3.W), stage.addr(28, 0)),
          stage.addr)
        cache.uncached := stage.addr(31, 28) >= "ha".U && stage.addr(31, 28) <= "hb".U
      }
    }
  }
}

object MMU extends App {
  implicit val config = new Config
  println(getVerilogString(new MMU()))
  // (new ChiselStage).emitVerilog(new MMU(), Array("--target-dir", "output/"))
}
