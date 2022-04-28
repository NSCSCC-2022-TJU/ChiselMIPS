package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

// IO端口定义 IO port definition
class CP02Stage_IO() extends Bundle {
  val flush = outputBool
}

class CP02IF_IO() extends Bundle {
  val excAddr = outputUInt(machine_width)
}

class CP02EXE_IO() extends Bundle {
  val raddr = inputUInt(regAddr_width)
  val data  = outputUInt(machine_width)
}

class CP02MEM_IO(implicit config: Config) extends Bundle {
  val pc       = inputUInt(machine_width)
  val inDelay  = inputBool
  val excCode  = inputUInt(excCode_width)
  val badVaddr = inputUInt(machine_width)
  val status   = outputUInt(machine_width)
  val cause    = outputUInt(machine_width)
}

class CP02WB_IO(implicit config: Config) extends Bundle {
  val we          = inputBool
  val waddr       = inputUInt(regAddr_width)
  val wdata       = inputUInt(machine_width)
  val tlbOp       = if (config.hasTLB) Some(Input(TlbOp())) else None
  val refetchFlag = if (config.hasTLB) Some(inputBool) else None
  val pc          = if (config.hasTLB) Some(inputUInt(machine_width)) else None
}

class CP0IO(implicit config: Config) extends Bundle {
  val int     = inputUInt(int_width)
  val flushio = new CP02Stage_IO
  val ifio    = new CP02IF_IO
  val exeio   = new CP02EXE_IO
  val memio   = new CP02MEM_IO
  val wbio    = new CP02WB_IO
  // TLB2CP0 定义在TLB中 TLB2CP0 is defined in TLB
  val mmuio   = if (config.hasTLB) Some(Flipped(new MMU2CP0_IO())) else None
}

// 寄存器Bundle定义 reg bundle definition
class CauseBundle extends Bundle {
  val bd      = UInt(1.W)
  val ti      = UInt(1.W)
  val none0   = UInt(14.W)
  val ip7to2  = UInt(6.W)
  val ip1to0  = UInt(2.W)
  val none1   = UInt(1.W)
  val excCode = UInt(5.W)
  val none2   = UInt(2.W)
}

class StatusBundle extends Bundle {
  val none0 = UInt(9.W)
  val bev   = UInt(1.W)
  val none1 = UInt(6.W)
  val im    = UInt(8.W)
  val none2 = UInt(6.W)
  val exl   = UInt(1.W)
  val ie    = UInt(1.W)
}

class EntryHiBundle extends Bundle {
  val vpn2  = UInt(19.W)
  val none0 = UInt(5.W)
  val asid  = UInt(8.W)
}

class EntryLoBundle extends Bundle {
  val none0   = UInt(6.W)
  val pfn_cdv = new PFN_CDV_Bundle
  val g       = UInt(1.W)
}

class IndexBundle(implicit config: Config) extends Bundle {
  val p     = UInt(1.W)
  val none0 = UInt((31 - config.tlb_index_width).W)
  val index = UInt(config.tlb_index_width.W)
}

class ConfigRegBundle(implicit config: Config) extends Bundle {
  val m     = UInt(1.W)
  val none0 = UInt(15.W)
  val be    = UInt(1.W)
  val at    = UInt(2.W)
  val ar    = UInt(3.W)
  val mt    = UInt(3.W)
  val none2 = UInt(4.W)
  val k0    = UInt(3.W)

  def init = {
    m := {
      if (config.debug.cp0) 0.U else 1.U
    }
    mt := 1.U
    k0 := 2.U
  }
}

class Config1(implicit config: Config) extends Bundle {
  val m       = UInt(1.W)
  val mmuSize = UInt(6.W)
  val is      = UInt(3.W)
  val il      = UInt(3.W)
  val ia      = UInt(3.W)
  val ds      = UInt(3.W)
  val dl      = UInt(3.W)
  val da      = UInt(3.W)
  val c2      = UInt(1.W)
  val md      = UInt(1.W)
  val pc      = UInt(1.W)
  val wr      = UInt(1.W)
  val ca      = UInt(1.W)
  val ep      = UInt(1.W)
  val fp      = UInt(1.W)

  def init = {
    m := 0.U
    mmuSize := Math.pow(2, config.tlb_index_width).toInt.U
    is := {
      if (config.icacheConfig.index_width == 5) 7.U else (config.icacheConfig.index_width - 6).U
    }
    il := {
      if (!config.hasCache) 0.U else (config.icacheConfig.offset_width - 1).U
    }
    ia := (config.icacheConfig.association - 1).U
    ds := {
      if (config.dcacheConfig.index_width == 5) 7.U else (config.dcacheConfig.index_width - 6).U
    }
    dl := {
      if (!config.hasCache) 0.U else (config.dcacheConfig.offset_width - 1).U
    }
    da := (config.dcacheConfig.association - 1).U
  }
}

/**
 * tlbp\tlbr\tlbwi 均在写回阶段执行
 *
 * @param config
 */
class CP0(implicit config: Config) extends Module {
  // io端口 io
  val io = IO(new CP0IO())

  val ifio  = io.ifio
  val exeio = io.exeio
  val memio = io.memio
  val wbio  = io.wbio


  // 下述寄存器的写法虽然丑陋但可以避免只读为0的寄存器被声明为reg the following implement is ugly but with less reg
  // 经过实测，相比verilog实现，虽然reg减少，但cp0消耗的fpga资源反而增加，时序变差。
  val regBundle = new Bundle() {
    val badVaddr = UInt(machine_width.W)
    val count    = UInt(machine_width.W)
    val status   = new StatusBundle
    val cause    = new CauseBundle
    val epc      = UInt(machine_width.W)
    val compare  = UInt(machine_width.W)
    val index    = new IndexBundle()
    val entryhi  = new EntryHiBundle
    val entrylo0 = new EntryLoBundle
    val entrylo1 = new EntryLoBundle
    val config0  = new ConfigRegBundle // config reg
    val config1  = new Config1
  }

  val reg = Reg(regBundle)

  val countFlag = Reg(Bool())

  // 主体逻辑 main logic
  when(reset.asBool) {
    reg := 0.U.asTypeOf(regBundle)
    reg.status.bev := 1.U
    countFlag := 0.U
    if (config.hasTLB) {
      reg.config0.init
      reg.config1.init
    }
  }.otherwise {
    reg.cause.ti := reg.count === reg.compare && reg.compare =/= 0.U
    reg.cause.ip7to2 := io.int | Cat(reg.cause.ti, 0.U(5.W))

    when(countFlag) {
      reg.count := reg.count + 1.U
    }
    countFlag := !countFlag

    //    when(memio.excCode === excNone) {
    //      if (config.hasTLB) doWrite_hasTLB() else doWrite_noTLB()
    //    }.elsewhen(memio.excCode === excEret) {
    //      doEret()
    //    }.otherwise {
    //      doExc()
    //    }

    if (config.hasTLB) doWrite_hasTLB() else doWrite_noTLB()

    when(memio.excCode === excEret) {
      doEret()
    }.elsewhen(memio.excCode =/= excNone) {
      doExc()
    }
  }

  // 输出端口赋值 output port assignment
  io.flushio.flush := SoftMuxByConfig(
    config.hasTLB,
    (memio.excCode =/= excNone) || wbio.refetchFlag.get,
    memio.excCode =/= excNone
  )

  memio.cause := reg.cause.asUInt
  memio.status := reg.status.asUInt

  ifio.excAddr := SoftMuxByConfig(
    config.hasTLB,
    Mux(wbio.refetchFlag.get, wbio.pc.get + 4.U, MuxLookup(memio.excCode, config.excAddr, Array(
      excInt -> config.excIntAddr,
      excEret -> Mux(wbio.waddr === cp0_epc_addr && wbio.we === true.B, wbio.wdata, reg.epc),
      excTlbRefillL -> config.excTlbRefillAddr,
      excTlbRefillS -> config.excTlbRefillAddr
    ))),
    MuxLookup(memio.excCode, config.excAddr, Array(
      excInt -> config.excIntAddr,
      excEret -> Mux(wbio.waddr === cp0_epc_addr && wbio.we === true.B, wbio.wdata, reg.epc)
    ))
  )

  exeio.data := MuxLookup(exeio.raddr, reg.count,
    {
      val regs = Array(
        cp0_badVaddr_addr -> reg.badVaddr,
        cp0_status_addr -> reg.status.asUInt,
        cp0_cause_addr -> reg.cause.asUInt,
        cp0_epc_addr -> reg.epc,
        cp0_count_addr -> reg.count,
        cp0_compare_addr -> reg.compare
      )
      if (config.hasTLB) {
        regs ++ Array(
          cp0_index_addr -> reg.index.asUInt,
          cp0_entryhi_addr -> reg.entryhi.asUInt,
          cp0_entrylo0_addr -> reg.entrylo0.asUInt,
          cp0_entrylo1_addr -> reg.entrylo1.asUInt,
          cp0_config_addr -> reg.config0.asUInt)
      } else {
        regs
      }
    }
  )

  if (config.hasTLB) {
    val mmuio  = io.mmuio.get
    val tlbout = mmuio.cp02tlb
    tlbout.index := reg.index.index
    tlbout.vpn2_asid.vpn2 := reg.entryhi.vpn2
    tlbout.vpn2_asid.asid := reg.entryhi.asid
    tlbout.pfn_cdv0 := reg.entrylo0.pfn_cdv
    tlbout.pfn_cdv1 := reg.entrylo1.pfn_cdv
    tlbout.g := reg.entrylo0.g & reg.entrylo1.g
    mmuio.k0 := reg.config0.k0
  }

  def doWrite_hasTLB() = {
    when(wbio.we.asBool) {
      switch(wbio.waddr) {
        is(cp0_badVaddr_addr) {
          reg.badVaddr := wbio.wdata
        }
        is(cp0_status_addr) {
          val wdata = wbio.wdata.asTypeOf(new StatusBundle)
          reg.status.im := wdata.im
          reg.status.exl := wdata.exl
          reg.status.ie := wdata.ie
        }
        is(cp0_cause_addr) {
          val wdata = wbio.wdata.asTypeOf(new CauseBundle)
          reg.cause.ip1to0 := wdata.ip1to0
        }
        is(cp0_epc_addr) {
          reg.epc := wbio.wdata
        }
        is(cp0_count_addr) {
          reg.count := wbio.wdata
        }
        is(cp0_compare_addr) {
          reg.compare := wbio.wdata
          reg.cause.ti := 0.U
        }
        is(cp0_index_addr) {
          val wdata = wbio.wdata.asTypeOf(new IndexBundle())
          reg.index.index := wdata.index
          when(wbio.tlbOp.get.isOneOf(TlbOp.tlbp)) {
            reg.index.p := wdata.p
          }
        }
        is(cp0_entryhi_addr) {
          val wdata = wbio.wdata.asTypeOf(new EntryHiBundle)
          reg.entryhi.vpn2 := wdata.vpn2
          reg.entryhi.asid := wdata.asid
        }
        is(cp0_entrylo0_addr) {
          val wdata = wbio.wdata.asTypeOf(new EntryLoBundle)
          reg.entrylo0.pfn_cdv := wdata.pfn_cdv
          reg.entrylo0.g := wdata.g
        }
        is(cp0_entrylo1_addr) {
          val wdata = wbio.wdata.asTypeOf(new EntryLoBundle)
          reg.entrylo1.pfn_cdv := wdata.pfn_cdv
          reg.entrylo1.g := wdata.g
        }
        is(cp0_config_addr) {
          val wdata = wbio.wdata.asTypeOf(new ConfigRegBundle)
          reg.config0.k0 := wdata.k0
        }
      }
    }.elsewhen(wbio.tlbOp.get =/= TlbOp.tlbNone) {
      switch(wbio.tlbOp.get) {
        is(TlbOp.tlbr) {
          val mmuio = io.mmuio.get
          val tlbin = mmuio.tlb2cp0
          reg.entryhi.vpn2 := tlbin.vpn2_asid.vpn2
          reg.entryhi.asid := tlbin.vpn2_asid.asid
          reg.entrylo0.pfn_cdv := tlbin.pfn_cdv0
          reg.entrylo0.g := tlbin.g
          reg.entrylo1.pfn_cdv := tlbin.pfn_cdv1
          reg.entrylo1.g := tlbin.g
        }
      }
    }
  }

  def doWrite_noTLB() = {
    when(wbio.we.asBool) {
      switch(wbio.waddr) {
        is(cp0_badVaddr_addr) {
          reg.badVaddr := wbio.wdata
        }
        is(cp0_status_addr) {
          val wdata = wbio.wdata.asTypeOf(new StatusBundle)
          reg.status.im := wdata.im
          reg.status.exl := wdata.exl
          reg.status.ie := wdata.ie
        }
        is(cp0_cause_addr) {
          val wdata = wbio.wdata.asTypeOf(new CauseBundle)
          reg.cause.ip1to0 := wdata.ip1to0
        }
        is(cp0_epc_addr) {
          reg.epc := wbio.wdata
        }
        is(cp0_count_addr) {
          reg.count := wbio.wdata
        }
        is(cp0_compare_addr) {
          reg.compare := wbio.wdata
          reg.cause.ti := 0.U
        }
      }
    }
  }

  def doEret() = {
    reg.status.exl := 0.U
  }

  def doExc() = {
    when(reg.status.exl === 0.U) {
      when(memio.inDelay) {
        reg.cause.bd := 1.U
        reg.epc := memio.pc - 4.U
      }.otherwise {
        reg.cause.bd := 0.U
        reg.epc := memio.pc
      }
    }
    reg.status.exl := 1.U
    if (config.hasTLB) {
      reg.cause.excCode := MuxCase(memio.excCode, Seq(
        (memio.excCode === excTlbRefillL || memio.excCode === excTlbInvalidL) -> excTlbl,
        (memio.excCode === excTlbRefillS || memio.excCode === excTlbInvalidS) -> excTlbs,
      ))
      when(memio.excCode === excAdel || memio.excCode === excAdes
        || memio.excCode === excTlbRefillL || memio.excCode === excTlbInvalidL
        || memio.excCode === excTlbRefillS || memio.excCode === excTlbInvalidS
        || memio.excCode === excMod) {
        reg.badVaddr := memio.badVaddr
      }
      when(memio.excCode === excTlbRefillL || memio.excCode === excTlbInvalidL
        || memio.excCode === excTlbRefillS || memio.excCode === excTlbInvalidS
        || memio.excCode === excMod) {
        reg.entryhi.vpn2 := memio.badVaddr(31, 13)
      }
    } else {
      reg.cause.excCode := memio.excCode
      when(memio.excCode === excAdel || memio.excCode === excAdes) {
        reg.badVaddr := memio.badVaddr
      }
    }
  }

}

object CP0 extends App {
  implicit val config = new Config
  config.hasTLB = true
  println(getVerilogString(new CP0()))
  // (new ChiselStage).emitVerilog(new CP0(), Array("--target-dir", "output/"))
}
