package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

class ID2EXE_IO(implicit config: Config) extends Bundle {
  val byPass      = new ByPassIO()
  val mregEXE     = inputBool
  val indelay     = outputBool
  val pc          = outputUInt(machine_width)
  val excCode     = outputUInt(excCode_width)
  val aluOp       = Output(ALUOp())
  val aluType     = Output(ALUType())
  val whilo       = outputBool
  val mreg        = outputBool
  val waddr       = outputUInt(regAddr_width)
  val wreg        = outputBool
  val din         = outputUInt(machine_width)
  val retAddr     = outputUInt(machine_width)
  val cp0Addr     = outputUInt(regAddr_width)
  val src1        = outputUInt(machine_width)
  val src2        = outputUInt(machine_width)
  val refetchFlag = if (config.hasTLB) Some(outputBool) else None
}

class ID2MEM_IO extends Bundle {
  val byPass  = new ByPassIO()
  val mregMEM = inputBool
}

class ID2Cache_IO extends Bundle {
  val inst = inputUInt(machine_width)
}

class ID2RegFile_IO extends Bundle {
  val raddr1 = outputUInt(regAddr_width)
  val raddr2 = outputUInt(regAddr_width)
  val rdata1 = inputUInt(machine_width)
  val rdata2 = inputUInt(machine_width)
}

class StageIDIO(implicit config: Config) extends PipelineIO {
  val ifio      = Flipped(new IF2ID_IO)
  val exeio     = new ID2EXE_IO
  val memio     = new ID2MEM_IO
  val cacheio   = new ID2Cache_IO
  val regfileio = new ID2RegFile_IO
}

class StageID(implicit config: Config) extends Module {
  // IO
  val io        = IO(new StageIDIO)
  val flushio   = io.flushio
  val scuio     = io.scuio
  val ifio      = io.ifio
  val exeio     = io.exeio
  val memio     = io.memio
  val cacheio   = io.cacheio
  val regfileio = io.regfileio

  val flush = flushio.flush
  val stall = scuio.stall

  val needRefetch = Wire(Bool())
  val refetchFlag = Reg(Bool())

  // 流水线寄存器 pipeline register
  val regBundle = new Bundle() {
    val pc      = UInt(machine_width.W)
    val pc4     = UInt(machine_width.W)
    val excCode = UInt(excCode_width.W)
    val inst    = UInt(machine_width.W)
  }
  val reg       = Reg(regBundle)

  when(reset.asBool || flush) {
    reg := 0.U.asTypeOf(regBundle)
    reg.excCode := excNone
  }.elsewhen(stall(1) && !stall(2)) {
    reg := 0.U.asTypeOf(regBundle)
    reg.excCode := excNone
  }.elsewhen(!stall(1)) {
    reg.pc := ifio.pc
    reg.pc4 := ifio.pc4
    reg.excCode := ifio.excCode
    reg.inst := cacheio.inst
  }

  if (config.hasTLB) {
    when(reset.asBool || flush) {
      refetchFlag := false.B
    }.elsewhen(needRefetch) {
      refetchFlag := true.B
    }
  }

  // DCU
  val dcu   = Module(new DCU)
  val dcuio = dcu.io
  dcuio.inst := reg.inst

  val inst = reg.inst

  val rs         = inst(25, 21)
  val rt         = inst(20, 16)
  val rd         = inst(15, 11)
  val imm        = inst(15, 0)
  val sa         = inst(10, 6)
  val instrIndex = inst(25, 0)
  val ra1        = rs
  val ra2        = rt

  regfileio.raddr1 := ra1
  regfileio.raddr2 := ra2
  val rdata1 = regfileio.rdata1
  val rdata2 = regfileio.rdata2

  val immExt  = MyMuxWithPriority(imm.asTypeOf(UInt(machine_width.W)), Array(
    dcuio.upper -> (imm << 16),
    dcuio.sext -> imm.asTypeOf(SInt(machine_width.W)).asUInt
  ))
  val lesseq  = exeio.src1(31) | (!exeio.src1.orR)
  val great   = !lesseq
  val immJump = Cat(imm.asTypeOf(SInt(30.W)).asUInt, 0.U(2.W))

  val fwrd1 = MyMuxWithPriority(0.U, Array(
    exeio.byPass.hazard(ra1, dcuio.rreg1) -> 1.U,
    memio.byPass.hazard(ra1, dcuio.rreg1) -> 2.U,
    dcuio.rreg1 -> 3.U
  ))

  val fwrd2 = MyMuxWithPriority(0.U, Array(
    exeio.byPass.hazard(ra2, dcuio.rreg2) -> 1.U,
    memio.byPass.hazard(ra2, dcuio.rreg2) -> 2.U,
    dcuio.rreg2 -> 3.U
  ))

  if (config.hasTLB) {
    needRefetch := dcuio.aluOp.isOneOf(ALUOp.TLBWI, ALUOp.TLBR) || (dcuio.aluOp.isOneOf(ALUOp.MTC0) && exeio.cp0Addr === cp0_entryhi_addr)
    ifio.refetchFlag.get := refetchFlag
    exeio.refetchFlag.get := needRefetch
  } else {
    needRefetch := DontCare
  }

  scuio.stallReq := MyMuxWithPriority(false.B, Array(
    ((exeio.byPass.hazard(ra1, dcuio.rreg1) || exeio.byPass.hazard(ra2, dcuio.rreg2)) && exeio.mregEXE) -> true.B,
    ((memio.byPass.hazard(ra1, dcuio.rreg1) || memio.byPass.hazard(ra2, dcuio.rreg2)) && memio.mregMEM) -> true.B,
    (dcuio.aluOp.isOneOf(ALUOp.JR, ALUOp.JALR) && fwrd1 === 1.U) -> true.B
  ))

  exeio.waddr := MyMuxWithPriority(rd, Array(
    dcuio.rtsel -> rt,
    dcuio.jal -> "b11111".U
  ))

  exeio.din := MyMux(fwrd2, Array(
    1.U -> exeio.byPass.wdata.get,
    2.U -> memio.byPass.wdata.get,
    3.U -> rdata2
  ))

  exeio.src1 := Mux(dcuio.shift, sa.asTypeOf(UInt(machine_width.W)), MyMux(fwrd1, Array(
    1.U -> exeio.byPass.wdata.get,
    2.U -> memio.byPass.wdata.get,
    3.U -> rdata1
  )))

  exeio.src2 := Mux(dcuio.immsel, immExt, MyMux(fwrd2, Array(
    1.U -> exeio.byPass.wdata.get,
    2.U -> memio.byPass.wdata.get,
    3.U -> rdata2
  )))

  exeio.excCode := MyMuxWithPriority(excNone, Array(
    (reg.excCode =/= excNone) -> reg.excCode,
    dcuio.aluOp.isOneOf(ALUOp.UNKNOWN) -> excRi,
    dcuio.aluOp.isOneOf(ALUOp.SYSCALL) -> excSys,
    dcuio.aluOp.isOneOf(ALUOp.ERET) -> excEret,
    dcuio.aluOp.isOneOf(ALUOp.BREAK) -> excBreak,
  ))

  exeio.pc := reg.pc
  exeio.retAddr := reg.pc4 + 4.U
  ifio.jumpAddr0 := Cat(Seq(reg.pc4(31, 28), instrIndex, 0.U(2.W)))
  ifio.jumpAddr1 := exeio.src1
  ifio.jumpAddr2 := reg.pc4 + immJump

  ifio.jtsel := MyMuxWithPriority(JTSel.PC4, Array(
    dcuio.aluOp.isOneOf(ALUOp.JAL, ALUOp.J) -> JTSel.Addr0,
    dcuio.aluOp.isOneOf(ALUOp.JR, ALUOp.JALR) -> JTSel.Addr1,
    ((dcuio.aluOp.isOneOf(ALUOp.BEQ) && exeio.src1 === exeio.src2) ||
      (dcuio.aluOp.isOneOf(ALUOp.BNE) && exeio.src1 =/= exeio.src2) ||
      (dcuio.aluOp.isOneOf(ALUOp.BGEZ, ALUOp.BGEZAL) && !exeio.src1(31)) ||
      (dcuio.aluOp.isOneOf(ALUOp.BLTZ, ALUOp.BLTZAL) && exeio.src1(31)) ||
      (dcuio.aluOp.isOneOf(ALUOp.BLEZ) && lesseq) ||
      (dcuio.aluOp.isOneOf(ALUOp.BGTZ) && great)) -> JTSel.Addr2,
  ))

  exeio.indelay := ShiftRegister(dcuio.jump, 1, !stall(2))

  exeio.aluOp <> dcuio.aluOp
  exeio.aluType <> dcuio.aluType
  exeio.wreg <> dcuio.wreg
  exeio.mreg <> dcuio.mreg
  exeio.whilo <> dcuio.whilo

  exeio.cp0Addr := rd
}

object StageID extends App {
  implicit val config = new Config
  println(getVerilogString(new StageID()))
  // (new ChiselStage).emitVerilog(new StageID(), Array("--target-dir", "output/"))
}
