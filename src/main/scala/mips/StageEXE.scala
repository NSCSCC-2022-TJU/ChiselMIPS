package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

class EXE2MEM_IO(implicit config: Config) extends Bundle {
  val cp0Bypass = if (config.hasCP0Bypass) new ByPassIO() else new ByPassIO(hasData = false)
  val aluOp     = Output(ALUOp())
  val excCode   = outputUInt(excCode_width)
  val pc        = outputUInt(machine_width)
  val indelay   = outputBool
  val mreg      = outputBool
  val waddr     = outputUInt(regAddr_width)
  val wreg      = outputBool
  val din       = outputUInt(machine_width)
  val wdata     = outputUInt(machine_width)
  val cp0we     = outputBool
  val cp0waddr  = outputUInt(regAddr_width)
  val cp0wdata  = outputUInt(machine_width)

  val hasExc      = inputBool //访存阶段是否需要异常处理
  val refetchFlag = if (config.hasTLB) Some(outputBool) else None
}

class EXE2WB_IO(implicit config: Config) extends Bundle {
  val cp0Bypass = if (config.hasCP0Bypass) new ByPassIO() else new ByPassIO(hasData = false)
}

class StageEXEIO(implicit config: Config) extends PipelineIO {
  val idio  = Flipped(new ID2EXE_IO)
  val memio = new EXE2MEM_IO
  val wbio  = new EXE2WB_IO
  val cp0io = Flipped(new CP02EXE_IO)
}

class StageEXE(implicit config: Config) extends Module {
  // IO
  val io    = IO(new StageEXEIO)
  val idio  = io.idio
  val memio = io.memio
  val wbio  = io.wbio
  val cp0io = io.cp0io
  val flush = io.flushio.flush
  val scuio = io.scuio
  val stall = scuio.stall


  // 流水线寄存器 pipeline register
  // todo 写法有待优化
  val regBundle = new Bundle() {
    val pc      = UInt(machine_width.W)
    val aluOp   = ALUOp()
    val src1    = UInt(machine_width.W)
    val src2    = UInt(machine_width.W)
    val excCode = UInt(excCode_width.W)
    val aluType = ALUType()
    val whilo   = Bool()
    val mreg    = Bool()
    val waddr   = UInt(regAddr_width.W)
    val wreg    = Bool()
    val din     = UInt(machine_width.W)
    val retAddr = UInt(machine_width.W)
    val cp0Addr = UInt(regAddr_width.W)
    val indelay = Bool()
  }
  val reg       = Reg(regBundle)

  when(reset.asBool || flush) {
    reg := 0.U.asTypeOf(regBundle)
    reg.aluOp := ALUOp.SLL
    reg.excCode := excNone
  }.elsewhen(stall(2) && !stall(3)) {
    reg := 0.U.asTypeOf(regBundle)
    reg.aluOp := ALUOp.SLL
    reg.excCode := excNone
  }.elsewhen(!stall(2)) {
    // todo 写法有待优化
    reg.pc := idio.pc
    reg.aluOp := idio.aluOp
    reg.src1 := idio.src1
    reg.src2 := idio.src2
    reg.excCode := idio.excCode
    reg.aluType := idio.aluType
    reg.whilo := idio.whilo
    reg.mreg := idio.mreg
    reg.waddr := idio.waddr
    reg.wreg := idio.wreg
    reg.din := idio.din
    reg.retAddr := idio.retAddr
    reg.cp0Addr := idio.cp0Addr
    reg.indelay := idio.indelay
  }

  val pc      = reg.pc
  val aluOp   = reg.aluOp
  val src1    = reg.src1
  val src2    = reg.src2
  val excCode = reg.excCode
  val aluType = reg.aluType
  val whilo   = reg.whilo
  val mreg    = reg.mreg
  val waddr   = reg.waddr
  val wreg    = reg.wreg
  val din     = reg.din
  val retAddr = reg.retAddr
  val cp0Addr = reg.cp0Addr
  val indelay = reg.indelay

  if (config.hasTLB) {
    val refetchFlag = Reg(Bool())
    when(reset.asBool || flush) {
      refetchFlag := false.B
    }.elsewhen(stall(2) && !stall(3)) {
      refetchFlag := false.B
    }.elsewhen(!stall(2)) {
      refetchFlag := idio.refetchFlag.get
    }
    memio.refetchFlag.get := refetchFlag
  }

  // ALU
  val alu   = Module(new ALU)
  val aluio = alu.io
  aluio.src1 := src1
  aluio.src2 := src2
  aluio.aluOp := aluOp

  val outHilo = MyMux(aluOp, Array(
    ALUOp.MULTU -> aluio.outMul,
    ALUOp.MULT -> aluio.outMul,
    ALUOp.DIV -> aluio.outDiv,
    ALUOp.DIVU -> aluio.outDiv,
  ))

  // hilo寄存器 hilo register
  // hilo寄存器在EXE级完成读写
  val hilo = Reg(new Bundle() {
    val hi = UInt(machine_width.W)
    val lo = UInt(machine_width.W)
  })

  when(reset.asBool) {
    hilo := 0.U.asTypeOf(hilo)
  }.elsewhen(whilo && !memio.hasExc) {
    when(aluOp.isOneOf(ALUOp.MTHI)) {
      hilo.hi := src1
    }.elsewhen(aluOp.isOneOf(ALUOp.MTLO)) {
      hilo.lo := src1
    }.otherwise {
      hilo := outHilo.asTypeOf(hilo)
    }
  }


  val cp0rdata = SoftMuxByConfig(
    config.hasCP0Bypass,
    MyMuxWithPriority(cp0io.data, Array(
      memio.cp0Bypass.hazard(cp0io.raddr) -> memio.cp0Bypass.wdata.get,
      wbio.cp0Bypass.hazard(cp0io.raddr) -> wbio.cp0Bypass.wdata.get,
    )),
    cp0io.data
  )

  val outMove = MyMux(aluOp, Array(
    ALUOp.MFHI -> hilo.hi,
    ALUOp.MFLO -> hilo.lo,
    ALUOp.MFC0 -> cp0rdata,
  ))


  scuio.stallReq := SoftMuxByConfig(
    config.hasCP0Bypass,
    aluio.stallReq,
    MyMuxWithPriority(aluio.stallReq, Array(
      memio.cp0Bypass.hazard(cp0io.raddr, aluOp.isOneOf(ALUOp.MFC0)) -> true.B,
      wbio.cp0Bypass.hazard(cp0io.raddr, aluOp.isOneOf(ALUOp.MFC0)) -> true.B,
    ))
  )

  memio.excCode := MyMuxWithPriority(excNone, Array(
    (excCode =/= excNone) -> excCode,
    (aluOp.isOneOf(ALUOp.ADD, ALUOp.SUB) && aluio.ov) -> excOv
  ))

  {
    val table = Array(
      ALUType.Mul -> aluio.outMul(machine_width - 1, 0),
      ALUType.Arith -> aluio.outArith,
      ALUType.Logic -> aluio.outLogic,
      ALUType.Shift -> aluio.outShift,
      ALUType.Mov -> outMove,
      ALUType.JAL -> retAddr,
      ALUType.TLBP -> cp0rdata
    )
    memio.wdata := MyMux(aluType, if (config.hasTLB) table else table.dropRight(1))
  }


  memio.aluOp := aluOp
  memio.mreg := mreg
  memio.din := din
  memio.pc := pc
  memio.indelay := indelay

  memio.cp0we := SoftMuxByConfig(
    config.hasTLB,
    aluOp.isOneOf(ALUOp.MTC0, ALUOp.TLBP),
    aluOp.isOneOf(ALUOp.MTC0)
  )
  memio.cp0wdata := src2

  memio.cp0waddr := SoftMuxByConfig(
    config.hasTLB,
    Mux(aluOp.isOneOf(ALUOp.TLBP), cp0_index_addr, cp0Addr),
    cp0Addr
  )

  memio.waddr := waddr
  memio.wreg := wreg

  cp0io.raddr := SoftMuxByConfig(
    config.hasTLB,
    Mux(aluOp.isOneOf(ALUOp.TLBP), cp0_entryhi_addr, cp0Addr),
    cp0Addr
  )

  idio.byPass.wreg := wreg
  idio.byPass.waddr := waddr
  idio.byPass.wdata.get := memio.wdata
  idio.mregEXE := mreg
}

object StageEXE extends App {
  implicit val config = new Config
  println(getVerilogString(new StageEXE()))
  // (new ChiselStage).emitVerilog(new StageEXE(), Array("--target-dir", "output/"))
}
