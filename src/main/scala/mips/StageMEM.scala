package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

class MEM2WB_IO(implicit config: Config) extends Bundle {
  val cp0Bypass   = if (config.hasCP0Bypass) new ByPassIO() else new ByPassIO(hasData = false)
  val aluOp       = Output(ALUOp())
  val pc          = outputUInt(machine_width)
  val mreg        = outputBool
  val waddr       = outputUInt(regAddr_width)
  val wreg        = outputBool
  val wdata       = outputUInt(machine_width)
  val dre         = outputUInt(4)
  val cp0we       = outputBool
  val cp0waddr    = outputUInt(regAddr_width)
  val cp0wdata    = outputUInt(machine_width)
  val pcWB        = inputUInt(machine_width)
  val refetchFlag = if (config.hasTLB) Some(outputBool) else None
}

// todo 有待精简
class MEM2Cache_IO extends Bundle {
  val req    = outputBool
  val we     = outputUInt(4)
  val din    = outputUInt(machine_width)
  val dataOk = inputBool
}

class StageMEMIO(implicit config: Config) extends PipelineIO {
  val idio    = Flipped(new ID2MEM_IO)
  val exeio   = Flipped(new EXE2MEM_IO)
  val cp0io   = Flipped(new CP02MEM_IO)
  val wbio    = new MEM2WB_IO()
  val cacheio = new MEM2Cache_IO
  val mmuio   = Flipped(new MMU2EME_IO())
}

class StageMEM(implicit config: Config) extends Module {
  // IO
  val io      = IO(new StageMEMIO)
  val idio    = io.idio
  val exeio   = io.exeio
  val cp0io   = io.cp0io
  val wbio    = io.wbio
  val cacheio = io.cacheio
  val flush   = io.flushio.flush
  val scuio   = io.scuio
  val stall   = scuio.stall
  val mmuio   = io.mmuio

  // 流水线寄存器 pipeline register
  val regBundle = new Bundle() {
    val pc       = UInt(machine_width.W)
    val aluOp    = ALUOp()
    val excCode  = UInt(excCode_width.W)
    val mreg     = Bool()
    val waddr    = UInt(regAddr_width.W)
    val wreg     = Bool()
    val wdata    = UInt(machine_width.W)
    val din      = UInt(machine_width.W)
    val indelay  = Bool()
    val cp0we    = Bool()
    val cp0waddr = UInt(regAddr_width.W)
    val cp0wdata = UInt(machine_width.W)
  }
  val reg       = Reg(regBundle)

  when(reset.asBool || flush) {
    reg := 0.U.asTypeOf(regBundle)
    reg.aluOp := ALUOp.SLL
    reg.excCode := excNone
  }.elsewhen(stall(3) && !stall(4)) {
    reg := 0.U.asTypeOf(regBundle)
    reg.aluOp := ALUOp.SLL
    reg.excCode := excNone
  }.elsewhen(!stall(3)) {
    // todo 写法有待优化
    reg.pc := exeio.pc
    reg.aluOp := exeio.aluOp
    reg.excCode := exeio.excCode
    reg.mreg := exeio.mreg
    reg.waddr := exeio.waddr
    reg.wreg := exeio.wreg
    reg.wdata := exeio.wdata
    reg.din := exeio.din
    reg.indelay := exeio.indelay
    reg.cp0we := exeio.cp0we
    reg.cp0waddr := exeio.cp0waddr
    reg.cp0wdata := exeio.cp0wdata
  }

  val pc       = reg.pc
  val aluOp    = reg.aluOp
  val excCode  = reg.excCode
  val mreg     = reg.mreg
  val waddr    = reg.waddr
  val wreg     = reg.wreg
  val wdata    = reg.wdata
  val din      = reg.din
  val indelay  = reg.indelay
  val cp0we    = reg.cp0we
  val cp0waddr = reg.cp0waddr
  val cp0wdata = reg.cp0wdata

  if (config.hasTLB) {
    val refetchFlag = Reg(Bool())
    when(reset.asBool || flush) {
      refetchFlag := false.B
    }.elsewhen(stall(3) && !stall(4)) {
      refetchFlag := false.B
    }.elsewhen(!stall(3)) {
      refetchFlag := exeio.refetchFlag.get
    }
    wbio.refetchFlag.get := refetchFlag
  }


  val status = SoftMuxByConfig(
    config.hasCP0Bypass,
    Mux(wbio.cp0Bypass.hazard(cp0_status_addr), wbio.cp0Bypass.wdata.get, cp0io.status),
    cp0io.status)

  val cause = SoftMuxByConfig(
    config.hasCP0Bypass,
    Mux(wbio.cp0Bypass.hazard(cp0_cause_addr), wbio.cp0Bypass.wdata.get, cp0io.cause),
    cp0io.cause
  )

  val daddr = wdata

  wbio.dre := MuxCase(0.U, Array(
    (aluOp.isOneOf(ALUOp.LB, ALUOp.LBU) && daddr(1, 0) === 0.U) -> "b0001".U,
    (aluOp.isOneOf(ALUOp.LB, ALUOp.LBU) && daddr(1, 0) === 1.U) -> "b0010".U,
    (aluOp.isOneOf(ALUOp.LB, ALUOp.LBU) && daddr(1, 0) === 2.U) -> "b0100".U,
    (aluOp.isOneOf(ALUOp.LB, ALUOp.LBU) && daddr(1, 0) === 3.U) -> "b1000".U,
    (aluOp.isOneOf(ALUOp.LH, ALUOp.LHU) && daddr(1, 0) === 0.U) -> "b0011".U,
    (aluOp.isOneOf(ALUOp.LH, ALUOp.LHU) && daddr(1, 0) === 2.U) -> "b1100".U,
    (aluOp.isOneOf(ALUOp.LW) && daddr(1, 0) === 0.U) -> "b1111".U,
  ))

  cacheio.we := MuxCase(0.U, Array(
    (aluOp.isOneOf(ALUOp.SB) && daddr(1, 0) === 0.U) -> "b0001".U,
    (aluOp.isOneOf(ALUOp.SB) && daddr(1, 0) === 1.U) -> "b0010".U,
    (aluOp.isOneOf(ALUOp.SB) && daddr(1, 0) === 2.U) -> "b0100".U,
    (aluOp.isOneOf(ALUOp.SB) && daddr(1, 0) === 3.U) -> "b1000".U,
    (aluOp.isOneOf(ALUOp.SH) && daddr(1, 0) === 0.U) -> "b0011".U,
    (aluOp.isOneOf(ALUOp.SH) && daddr(1, 0) === 2.U) -> "b1100".U,
    (aluOp.isOneOf(ALUOp.SW) && daddr(1, 0) === 0.U) -> "b1111".U,
  ))

  val dinByte = Fill(4, din(7, 0))
  val dinHalf = Fill(2, din(15, 0))
  cacheio.din := MuxLookup(cacheio.we, 0.U, Array(
    "b1111".U -> din,
    "b1100".U -> dinHalf,
    "b0011".U -> dinHalf,
    "b0001".U -> dinByte,
    "b0010".U -> dinByte,
    "b0100".U -> dinByte,
    "b1000".U -> dinByte,
  ))

  scuio.stallReq := SoftMuxByConfig(
    config.hasCP0Bypass,
    !cacheio.dataOk,
    (wbio.cp0Bypass.hazard(cp0_cause_addr) || wbio.cp0Bypass.hazard(cp0_status_addr)) || !cacheio.dataOk)

  wbio.aluOp := aluOp
  wbio.mreg := mreg
  wbio.waddr := waddr
  wbio.wreg := wreg
  wbio.wdata := wdata
  wbio.cp0we := cp0we
  wbio.cp0waddr := cp0waddr

  val index = Wire(new IndexBundle())
  index := 0.U.asTypeOf(new IndexBundle())
  if (config.hasTLB) {
    index.index := mmuio.index.get
    index.p := !mmuio.found.get
    index.none0 := 0.U
  }

  wbio.cp0wdata := SoftMuxByConfig(
    config.hasTLB,
    Mux(aluOp.isOneOf(ALUOp.TLBP), index.asUInt, cp0wdata),
    cp0wdata)

  wbio.pc := pc

  mmuio.addr := Mux(
    aluOp.isOneOf(ALUOp.TLBP),
    Cat(wdata.asTypeOf(new EntryHiBundle).vpn2, 0.U((machine_width - 19).W)),
    daddr
  )

  val excCodeArray = if (config.hasTLB) {
    val loadMux  = MuxCase(excNone, Seq(
      (!mmuio.found.get && mmuio.mapped.get) -> excTlbRefillL,
      (!mmuio.v.get && mmuio.mapped.get) -> excTlbInvalidL)
    )
    val storeMux = MuxCase(excNone, Seq(
      (!mmuio.found.get && mmuio.mapped.get) -> excTlbRefillS,
      (!mmuio.v.get && mmuio.mapped.get) -> excTlbInvalidS,
      (!mmuio.d.get && mmuio.mapped.get) -> excMod)
    )
    Array(
      (wbio.cp0Bypass.hazard(cp0_cause_addr) || wbio.cp0Bypass.hazard(cp0_status_addr)) -> excNone,
      ((status(15, 8) & cause(15, 8)) =/= 0.U && status(1) === 0.U && status(0) === 1.U) -> excInt,
      (excCode =/= excNone) -> excCode,
      ((aluOp.isOneOf(ALUOp.LH, ALUOp.LHU) && daddr(0) =/= 0.U) || aluOp.isOneOf(ALUOp.LW) && daddr(1, 0) =/= 0.U) -> excAdel,
      ((aluOp.isOneOf(ALUOp.SH) && daddr(0) =/= 0.U) || (aluOp.isOneOf(ALUOp.SW) && daddr(1, 0) =/= 0.U)) -> excAdes,
      aluOp.isOneOf(ALUOp.LB, ALUOp.LBU, ALUOp.LH, ALUOp.LHU, ALUOp.LW) -> loadMux,
      aluOp.isOneOf(ALUOp.SB, ALUOp.SH, ALUOp.SW) -> storeMux
    )
  } else {
    Array(
      (wbio.cp0Bypass.hazard(cp0_cause_addr) || wbio.cp0Bypass.hazard(cp0_status_addr)) -> excNone,
      ((status(15, 8) & cause(15, 8)) =/= 0.U && status(1) === 0.U && status(0) === 1.U) -> excInt,
      (excCode =/= excNone) -> excCode,
      ((aluOp.isOneOf(ALUOp.LH, ALUOp.LHU) && daddr(0) =/= 0.U) || aluOp.isOneOf(ALUOp.LW) && daddr(1, 0) =/= 0.U) -> excAdel,
      ((aluOp.isOneOf(ALUOp.SH) && daddr(0) =/= 0.U) || (aluOp.isOneOf(ALUOp.SW) && daddr(1, 0) =/= 0.U)) -> excAdes,
    )
  }

  cp0io.excCode := MyMuxWithPriority(excNone, if (config.hasCP0Bypass) excCodeArray.tail else excCodeArray)
  cp0io.pc := Mux(cp0io.excCode === excInt, wbio.pcWB + 4.U, pc)

  cp0io.badVaddr := SoftMuxByConfig(
    config.hasTLB,
    MyMuxWithPriority(config.pcInit, Array(
      (excCode === excAdel || excCode === excTlbRefillL || excCode === excTlbInvalidL) -> pc,
      (cp0io.excCode === excAdel || cp0io.excCode === excAdes
        || cp0io.excCode === excTlbRefillL || cp0io.excCode === excTlbRefillS
        || cp0io.excCode === excTlbInvalidL || cp0io.excCode === excTlbInvalidS
        || cp0io.excCode === excMod) -> daddr
    )),
    MyMuxWithPriority(config.pcInit, Array(
      (excCode === excAdel) -> pc,
      (cp0io.excCode === excAdel || cp0io.excCode === excAdes) -> daddr
    )))
  cp0io.inDelay := indelay

  cacheio.req := MyMuxWithPriority(0.U, Array(
    (cp0io.excCode =/= excNone) -> 0.U,
    aluOp.isOneOf(ALUOp.LB, ALUOp.LW, ALUOp.SB, ALUOp.SW, ALUOp.LBU, ALUOp.LH, ALUOp.LHU, ALUOp.SH) -> cacheio.dataOk
  ))

  idio.byPass.waddr := waddr
  idio.byPass.wreg := wreg
  idio.byPass.wdata.get := wdata
  idio.mregMEM := mreg

  exeio.cp0Bypass.wreg := cp0we
  exeio.cp0Bypass.waddr := cp0waddr
  if (config.hasCP0Bypass) {
    exeio.cp0Bypass.wdata.get := cp0wdata
  }
  exeio.hasExc := cp0io.excCode =/= excNone

}

object StageMEM extends App {
  implicit val config = new Config
  println(getVerilogString(new StageMEM()))
  // (new ChiselStage).emitVerilog(new StageMEM(), Array("--target-dir", "output/"))
}
