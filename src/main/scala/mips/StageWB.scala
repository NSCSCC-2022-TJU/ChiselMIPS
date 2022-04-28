package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

class WB2RegFile_IO extends Bundle {
  val wen   = outputBool
  val waddr = outputUInt(regAddr_width)
  val wdata = outputUInt(machine_width)
}

class WB2Cache_IO extends Bundle {
  val rdata  = inputUInt(machine_width)
  val dataOk = inputBool
}

class StageWBIO(implicit config: Config) extends Bundle {
  val memio     = Flipped(new MEM2WB_IO)
  val exeio     = Flipped(new EXE2WB_IO)
  val cp0io     = Flipped(new CP02WB_IO)
  val flush     = inputBool
  val stall     = inputUInt(stall_width)
  val cacheio   = new WB2Cache_IO
  val regfileio = new WB2RegFile_IO
  val mmuio     = if (config.hasTLB) Some(Flipped(new MMU2WB_IO)) else None
}

class StageWB(implicit config: Config) extends Module {
  val io        = IO(new StageWBIO)
  val memio     = io.memio
  val exeio     = io.exeio
  val cp0io     = io.cp0io
  val cacheio   = io.cacheio
  val regfileio = io.regfileio
  val flush     = io.flush
  val stall     = io.stall


  // 流水线寄存器 pipeline register
  val regBundle = new Bundle() {
    val pc       = UInt(machine_width.W)
    val aluOp    = ALUOp()
    val mreg     = Bool()
    val waddr    = UInt(regAddr_width.W)
    val wreg     = Bool()
    val wdata    = UInt(machine_width.W)
    val dre      = UInt(4.W)
    val cp0we    = Bool()
    val cp0waddr = UInt(regAddr_width.W)
    val cp0wdata = UInt(machine_width.W)
  }
  val reg       = Reg(regBundle)

  when(reset.asBool || flush) {
    reg := 0.U.asTypeOf(regBundle)
    reg.aluOp := ALUOp.SLL
  }.elsewhen(!stall(4)) {
    // todo 写法有待优化
    reg.pc := memio.pc
    reg.aluOp := memio.aluOp
    reg.mreg := memio.mreg
    reg.waddr := memio.waddr
    reg.wreg := memio.wreg
    reg.wdata := memio.wdata
    reg.dre := memio.dre
    reg.cp0we := memio.cp0we
    reg.cp0waddr := memio.cp0waddr
    reg.cp0wdata := memio.cp0wdata
  }

  val pc       = reg.pc
  val aluOp    = reg.aluOp
  val mreg     = reg.mreg
  val waddr    = reg.waddr
  val wreg     = reg.wreg
  val wdata    = reg.wdata
  val dre      = reg.dre
  val cp0we    = reg.cp0we
  val cp0waddr = reg.cp0waddr
  val cp0wdata = reg.cp0wdata

  if (config.hasTLB) {
    val refetchFlag = Reg(Bool())
    when(reset.asBool || flush) {
      refetchFlag := false.B
    }.elsewhen(!stall(4)) {
      refetchFlag := memio.refetchFlag.get
    }
    cp0io.refetchFlag.get := refetchFlag
    cp0io.pc.get := pc
  }

  regfileio.waddr := waddr
  regfileio.wen := wreg && cacheio.dataOk

  exeio.cp0Bypass.wreg := cp0we && cacheio.dataOk
  exeio.cp0Bypass.waddr := cp0waddr
  if (config.hasCP0Bypass) exeio.cp0Bypass.wdata.get := cp0wdata

  memio.cp0Bypass.wreg := cp0we && cacheio.dataOk
  memio.cp0Bypass.waddr := cp0waddr
  if (config.hasCP0Bypass) memio.cp0Bypass.wdata.get := cp0wdata
  memio.pcWB := pc

  cp0io.we := cp0we && cacheio.dataOk
  cp0io.waddr := cp0waddr
  cp0io.wdata := cp0wdata
  if (config.hasTLB) {
    val mmuio = io.mmuio.get
    cp0io.tlbOp.get := MuxCase(TlbOp.tlbNone, Seq(
      aluOp.isOneOf(ALUOp.TLBR) -> TlbOp.tlbr,
      aluOp.isOneOf(ALUOp.TLBWI) -> TlbOp.tlbwi,
      aluOp.isOneOf(ALUOp.TLBP) -> TlbOp.tlbp
    ))
    mmuio.we := aluOp.isOneOf(ALUOp.TLBWI)
  }

  val unsigned = aluOp.isOneOf(ALUOp.LBU, ALUOp.LHU)

  val cacheData = MyMux(dre, Array(
    "b1111".U -> cacheio.rdata,
    "b1100".U -> Mux(unsigned, cacheio.rdata(31, 16), cacheio.rdata(31, 16).asTypeOf(SInt(machine_width.W)).asUInt),
    "b0011".U -> Mux(unsigned, cacheio.rdata(15, 0), cacheio.rdata(15, 0).asTypeOf(SInt(machine_width.W)).asUInt),
    "b1000".U -> Mux(unsigned, cacheio.rdata(31, 24), cacheio.rdata(31, 24).asTypeOf(SInt(machine_width.W)).asUInt),
    "b0100".U -> Mux(unsigned, cacheio.rdata(23, 16), cacheio.rdata(23, 16).asTypeOf(SInt(machine_width.W)).asUInt),
    "b0010".U -> Mux(unsigned, cacheio.rdata(15, 8), cacheio.rdata(15, 8).asTypeOf(SInt(machine_width.W)).asUInt),
    "b0001".U -> Mux(unsigned, cacheio.rdata(7, 0), cacheio.rdata(7, 0).asTypeOf(SInt(machine_width.W)).asUInt),
  ))

  regfileio.wdata := Mux(mreg, cacheData, wdata)
}

object StageWB extends App {
  implicit val config = new Config
  println(getVerilogString(new StageWB()))
  // (new ChiselStage).emitVerilog(new StageWB(), Array("--target-dir", "output/"))
}