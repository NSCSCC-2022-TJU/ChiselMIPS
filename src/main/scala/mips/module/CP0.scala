package mips.module

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import Config._
import chisel3.experimental.ChiselEnum

class CP0_TLB_RegIO_Out(indexWidth: Int) extends Bundle {
  val index    = outputUInt(indexWidth)
  val entryhi  = Output(new EntryHiBundle)
  val entrylo0 = Output(new EntryLoBundle)
  val entrylo1 = Output(new EntryLoBundle)
}

class CP0RegIO(indexWidth: Int) extends Bundle {
  val status = outputUInt(machine_width)
  val cause  = outputUInt(machine_width)
  val tlbIn  = Flipped(new CP0_TLB_RegIO_Out(indexWidth))
  val tlbOut = new CP0_TLB_RegIO_Out(indexWidth)
}

class CP0IO(indexWidth: Int) extends Bundle {
  val we       = inputBool
  val raddr    = inputUInt(cp0_regAddr_width)
  val waddr    = inputUInt(cp0_regAddr_width)
  val wdata    = inputUInt(machine_width)
  val int      = inputUInt(int_width)
  val pc       = inputUInt(machine_width)
  val inDelay  = inputBool
  val excCode  = inputUInt(excCode_width)
  val badVaddr = inputUInt(machine_width)
  val flush    = outputBool
  val excAddr  = outputUInt(machine_width)
  val data     = outputUInt(machine_width)
  val regIO    = new CP0RegIO(indexWidth)
  val tlbOp    = Input(TlbOp())
  val tlbFound = inputBool
}

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
  val none0 = UInt(6.W)
  val pfn   = UInt(20.W)
  val c     = UInt(3.W)
  val d     = UInt(1.W)
  val v     = UInt(1.W)
  val g     = UInt(1.W)
}

class IndexBundle(indexWidth: Int) extends Bundle {
  val p     = UInt(1.W)
  val none0 = UInt((31 - indexWidth).W)
  val index = UInt(indexWidth.W)
}

class CP0(implicit config: Config) extends Module {
  val indexWidth = config.tlb_index_width

  val io = IO(new CP0IO(indexWidth))

  //debug
  if (config.debug) {
    Seq(io).foreach(_ := DontCare)
  }

  // 下述寄存器的写法可以避免只读为0的寄存器被声明为reg，丑陋但有效 the following implement is ugly but with less reg
  val regBundle = new Bundle() {
    val badVaddr = UInt(machine_width.W)
    val count    = UInt(machine_width.W)
    val status   = new StatusBundle
    val cause    = new CauseBundle
    val epc      = UInt(machine_width.W)
    val index    = new IndexBundle(indexWidth)
    val entryhi  = new EntryHiBundle
    val entrylo0 = new EntryLoBundle
    val entrylo1 = new EntryLoBundle
  }
  val reg       = Reg(regBundle)

  def doWrite() = {
    when(io.we.asBool) {
      switch(io.waddr) {
        is(cp0_badVaddr_addr) {
          reg.badVaddr := io.wdata
        }
        is(cp0_status_addr) {
          val wdata = io.wdata.asTypeOf(new StatusBundle)
          reg.status.im := wdata.im
          reg.status.exl := wdata.exl
          reg.status.ie := wdata.ie
        }
        is(cp0_cause_addr) {
          val wdata = io.wdata.asTypeOf(new CauseBundle)
          reg.cause.ip1to0 := wdata.ip1to0
        }
        is(cp0_epc_addr) {
          reg.epc := io.wdata
        }
        is(cp0_count_addr) {
          reg.count := io.wdata
        }
        is(cp0_index_addr) {
          val wdata = io.wdata.asTypeOf(new IndexBundle(indexWidth))
          reg.index.index := wdata.index
        }
        is(cp0_entryhi_addr) {
          val wdata = io.wdata.asTypeOf(new EntryHiBundle)
          reg.entryhi.vpn2 := wdata.vpn2
          reg.entryhi.asid := wdata.asid
        }
        is(cp0_entrylo0_addr) {
          val wdata = io.wdata.asTypeOf(new EntryLoBundle)
          reg.entrylo0.pfn := wdata.pfn
          reg.entrylo0.c := wdata.c
          reg.entrylo0.d := wdata.d
          reg.entrylo0.v := wdata.v
          reg.entrylo0.g := wdata.g
        }
        is(cp0_entrylo1_addr) {
          val wdata = io.wdata.asTypeOf(new EntryLoBundle)
          reg.entrylo1.pfn := wdata.pfn
          reg.entrylo1.c := wdata.c
          reg.entrylo1.d := wdata.d
          reg.entrylo1.v := wdata.v
          reg.entrylo1.g := wdata.g
        }
      }
    }.elsewhen(io.tlbOp =/= TlbOp.none) {
      switch(io.tlbOp) {
        is(TlbOp.tlbp) {
          reg.index.p := !io.tlbFound
          reg.index.index := io.regIO.tlbIn.index
        }
        is(TlbOp.tlbr) {
          val tlbin = io.regIO.tlbIn
          reg.entryhi.vpn2 := tlbin.entryhi.vpn2
          reg.entryhi.asid := tlbin.entryhi.asid
          reg.entrylo0.pfn := tlbin.entrylo0.pfn
          reg.entrylo0.c :=   tlbin.entrylo0.c
          reg.entrylo0.d :=   tlbin.entrylo0.d
          reg.entrylo0.v :=   tlbin.entrylo0.v
          reg.entrylo0.g :=   tlbin.entrylo0.g
          reg.entrylo1.pfn := tlbin.entrylo1.pfn
          reg.entrylo1.c :=   tlbin.entrylo1.c
          reg.entrylo1.d :=   tlbin.entrylo1.d
          reg.entrylo1.v :=   tlbin.entrylo1.v
          reg.entrylo1.g :=   tlbin.entrylo1.g
        }
      }
    }
  }

  def doEret() = {
    reg.status.exl := 0.U
  }

  def doExc() = {
    when(reg.status.exl === 0.U) {
      when(io.inDelay) {
        reg.cause.bd := 1.U
        reg.epc := io.pc - 4.U
      }.otherwise {
        reg.cause.bd := 0.U
        reg.epc := io.pc
      }
    }
    reg.status.exl := 1.U
    reg.cause.excCode := io.excCode
    when(io.excCode === excAdel || io.excCode === excAdes) {
      reg.badVaddr := io.badVaddr
    }
  }

  val countFlag = Reg(Bool())

  when(reset.asBool) {
    reg := 0.U.asTypeOf(regBundle)
    reg.status.bev := 1.U
    countFlag := 0.U
  }.otherwise {
    reg.cause.ip7to2 := io.int

    when(countFlag) {
      reg.count := reg.count + 1.U
    }
    countFlag := !countFlag

    when(io.excCode === excNone) {
      doWrite()
    }.elsewhen(io.excCode === excEret) {
      doEret()
    }.otherwise {
      doExc()
    }

  }
  io.flush := io.excCode =/= excNone
  io.regIO.cause := reg.cause.asUInt
  io.regIO.status := reg.status.asUInt
  io.excAddr := MuxLookup(io.excCode, excAddr, Array(
    excInt -> excIntAddr,
    excEret -> Mux(io.waddr === cp0_epc_addr && io.we === true.B, io.wdata, reg.epc)
  ))
  io.data := MuxLookup(io.raddr, reg.count, Array(
    cp0_badVaddr_addr -> reg.badVaddr,
    cp0_status_addr -> reg.status.asUInt,
    cp0_cause_addr -> reg.cause.asUInt,
    cp0_epc_addr -> reg.epc,
    cp0_index_addr -> reg.index.asUInt,
    cp0_entryhi_addr -> reg.entryhi.asUInt,
    cp0_entrylo0_addr -> reg.entrylo0.asUInt,
    cp0_entrylo1_addr -> reg.entrylo1.asUInt
  ))
}

object TlbOp extends ChiselEnum {
  val none, tlbp, tlbr = Value
}


object CP0 extends App {
  implicit val config = new Config
  println(getVerilogString(new CP0()))
  // (new ChiselStage).emitVerilog(new CP0(), Array("--target-dir", "output/"))
}
