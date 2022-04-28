package mips

import Chisel.debug
import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._
import config.Instructions._

class DCUIO(implicit config: Config) extends Bundle {
  val inst    = inputUInt(machine_width)
  val wreg    = outputBool
  val aluOp   = Output(ALUOp())
  val aluType = Output(ALUType())
  val whilo   = outputBool
  val shift   = outputBool
  val immsel  = outputBool
  val rtsel   = outputBool
  val sext    = outputBool
  val upper   = outputBool
  val jal     = outputBool
  val mreg    = outputBool
  val jump    = outputBool
  val rreg1   = outputBool
  val rreg2   = outputBool
}

class DCU(implicit config: Config) extends Module {
  val io      = IO(new DCUIO)
  val signals = Seq(io.aluOp, io.aluType, io.rreg1, io.rreg2, io.wreg, io.whilo, io.shift, io.immsel, io.rtsel, io.sext, io.mreg)
  val decoder = ListLookup(io.inst, decode_default, decode_table)
  signals zip decoder foreach { case (s, d) => s := d }

  io.jal := io.aluOp.isOneOf(ALUOp.JAL, ALUOp.BGEZAL, ALUOp.BLTZAL)
  io.jump := io.aluOp.isOneOf(ALUOp.J, ALUOp.JR, ALUOp.JAL, ALUOp.JALR, ALUOp.BEQ, ALUOp.BNE, ALUOp.BGEZ, ALUOp.BGTZ,
    ALUOp.BLEZ, ALUOp.BLTZ, ALUOp.BGEZAL, ALUOp.BLTZAL)
  io.upper := io.aluOp === ALUOp.LUI

  if (config.debug.dcu) {

    val inst = Wire(new DCUDebugBundle)

    inst.add := io.inst === ADD
    inst.addi := io.inst === ADDI
    inst.addu := io.inst === ADDU
    inst.addiu := io.inst === ADDIU
    inst.sub := io.inst === SUB
    inst.subu := io.inst === SUBU
    inst.slt := io.inst === SLT
    inst.slti := io.inst === SLTI
    inst.sltu := io.inst === SLTU
    inst.sltiu := io.inst === SLTIU
    inst.mul := io.inst === MUL
    inst.div := io.inst === DIV
    inst.divu := io.inst === DIVU
    inst.mult := io.inst === MULT
    inst.multu := io.inst === MULTU
    inst.mfhi := io.inst === MFHI
    inst.mflo := io.inst === MFLO
    inst.mthi := io.inst === MTHI
    inst.mtlo := io.inst === MTLO
    inst.and := io.inst === AND
    inst.andi := io.inst === ANDI
    inst.lui := io.inst === LUI
    inst.nor := io.inst === NOR
    inst.or := io.inst === OR
    inst.ori := io.inst === ORI
    inst.xor := io.inst === XOR
    inst.xori := io.inst === XORI
    inst.sllv := io.inst === SLLV
    inst.sll := io.inst === SLL
    inst.srav := io.inst === SRAV
    inst.sra := io.inst === SRA
    inst.srlv := io.inst === SRLV
    inst.srl := io.inst === SRL
    inst.beq := io.inst === BEQ
    inst.bne := io.inst === BNE
    inst.bgez := io.inst === BGEZ
    inst.bgtz := io.inst === BGTZ
    inst.blez := io.inst === BLEZ
    inst.bltz := io.inst === BLTZ
    inst.bgezal := io.inst === BGEZAL
    inst.bltzal := io.inst === BLTZAL
    inst.j := io.inst === J
    inst.jal := io.inst === JAL
    inst.jr := io.inst === JR
    inst.jalr := io.inst === JALR
    inst.syscall := io.inst === SYSCALL
    inst.break := io.inst === BREAK
    inst.eret := io.inst === ERET
    inst.mfc0 := io.inst === MFC0
    inst.mtc0 := io.inst === MTC0
    inst.lb := io.inst === LB
    inst.lh := io.inst === LH
    inst.lw := io.inst === LW
    inst.lbu := io.inst === LBU
    inst.lhu := io.inst === LHU
    inst.sb := io.inst === SB
    inst.sh := io.inst === SH
    inst.sw := io.inst === SW
    inst.tlbp := io.inst === TLBP
    inst.tlbwi := io.inst === TLBWI
    inst.tlbr := io.inst === TLBR

    val debug = Module(new DebugInformation)
    debug.io.dcu := inst
    //    debug.io.dcu.add := inst.add
    //    debug.io.dcu.addi := inst.addi
    //    debug.io.dcu.addu := inst.addu
    //    debug.io.dcu.addiu := inst.addiu
    //    debug.io.dcu.sub := inst.sub
    //    debug.io.dcu.subu := inst.subu
    //    debug.io.dcu.slt := inst.slt
    //    debug.io.dcu.slti := inst.slti
    //    debug.io.dcu.sltu := inst.sltu
    //    debug.io.dcu.sltiu := inst.sltiu
    //    debug.io.dcu.mul := inst.mul
    //    debug.io.dcu.div := inst.div
    //    debug.io.dcu.divu := inst.divu
    //    debug.io.dcu.mult := inst.mult
    //    debug.io.dcu.multu := inst.multu
    //    debug.io.dcu.mfhi := inst.mfhi
    //    debug.io.dcu.mflo := inst.mflo
    //    debug.io.dcu.mthi := inst.mthi
    //    debug.io.dcu.mtlo := inst.mtlo
    //    debug.io.dcu.and := inst.and
    //    debug.io.dcu.andi := inst.andi
    //    debug.io.dcu.lui := inst.lui
    //    debug.io.dcu.nor := inst.nor
    //    debug.io.dcu.or := inst.or
    //    debug.io.dcu.ori := inst.ori
    //    debug.io.dcu.xor := inst.xor
    //    debug.io.dcu.xori := inst.xori
    //    debug.io.dcu.sllv := inst.sllv
    //    debug.io.dcu.sll := inst.sll
    //    debug.io.dcu.srav := inst.srav
    //    debug.io.dcu.sra := inst.sra
    //    debug.io.dcu.srlv := inst.srlv
    //    debug.io.dcu.srl := inst.srl
    //    debug.io.dcu.beq := inst.beq
    //    debug.io.dcu.bne := inst.bne
    //    debug.io.dcu.bgez := inst.bgez
    //    debug.io.dcu.bgtz := inst.bgtz
    //    debug.io.dcu.blez := inst.blez
    //    debug.io.dcu.bltz := inst.bltz
    //    debug.io.dcu.bgezal := inst.bgezal
    //    debug.io.dcu.bltzal := inst.bltzal
    //    debug.io.dcu.j := inst.j
    //    debug.io.dcu.jal := inst.jal
    //    debug.io.dcu.jr := inst.jr
    //    debug.io.dcu.jalr := inst.jalr
    //    debug.io.dcu.syscall := inst.syscall
    //    debug.io.dcu.break := inst.break
    //    debug.io.dcu.eret := inst.eret
    //    debug.io.dcu.mfc0 := inst.mfc0
    //    debug.io.dcu.mtc0 := inst.mtc0
    //    debug.io.dcu.lb := inst.lb
    //    debug.io.dcu.lh := inst.lh
    //    debug.io.dcu.lw := inst.lw
    //    debug.io.dcu.lbu := inst.lbu
    //    debug.io.dcu.lhu := inst.lhu
    //    debug.io.dcu.sb := inst.sb
    //    debug.io.dcu.sh := inst.sh
    //    debug.io.dcu.sw := inst.sw
    //    debug.io.dcu.tlbp := inst.tlbp
    //    debug.io.dcu.tlbwi := inst.tlbwi
    //    debug.io.dcu.tlbr := inst.tlbr
  }
}

object DCU extends App {
  implicit val config = new Config
  println(getVerilogString(new DCU()))
  // (new ChiselStage).emitVerilog(new DCU(), Array("--target-dir", "output/"))
}

class DCUDebugBundle extends Bundle {
  val add,
  addi,
  addu,
  addiu,
  sub,
  subu,
  slt,
  slti,
  sltu,
  sltiu,
  mul,
  div,
  divu,
  mult,
  multu,
  mfhi,
  mflo,
  mthi,
  mtlo,
  and,
  andi,
  lui,
  nor,
  or,
  ori,
  xor,
  xori,
  sllv,
  sll,
  srav,
  sra,
  srlv,
  srl,
  beq,
  bne,
  bgez,
  bgtz,
  blez,
  bltz,
  bgezal,
  bltzal,
  j,
  jal,
  jr,
  jalr,
  syscall,
  break,
  eret,
  mfc0,
  mtc0,
  lb,
  lh,
  lw,
  lbu,
  lhu,
  sb,
  sh,
  sw,
  tlbp,
  tlbwi,
  tlbr = Bool()
}