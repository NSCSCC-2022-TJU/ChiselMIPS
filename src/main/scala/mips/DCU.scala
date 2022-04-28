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
    val debug = Wire(new DCUDebugBundle)
    dontTouch(debug)
    debug.add := io.inst === ADD
    debug.addi := io.inst === ADDI
    debug.addu := io.inst === ADDU
    debug.addiu := io.inst === ADDIU
    debug.sub := io.inst === SUB
    debug.subu := io.inst === SUBU
    debug.slt := io.inst === SLT
    debug.slti := io.inst === SLTI
    debug.sltu := io.inst === SLTU
    debug.sltiu := io.inst === SLTIU
    debug.mul := io.inst === MUL
    debug.div := io.inst === DIV
    debug.divu := io.inst === DIVU
    debug.mult := io.inst === MULT
    debug.multu := io.inst === MULTU
    debug.mfhi := io.inst === MFHI
    debug.mflo := io.inst === MFLO
    debug.mthi := io.inst === MTHI
    debug.mtlo := io.inst === MTLO
    debug.and := io.inst === AND
    debug.andi := io.inst === ANDI
    debug.lui := io.inst === LUI
    debug.nor := io.inst === NOR
    debug.or := io.inst === OR
    debug.ori := io.inst === ORI
    debug.xor := io.inst === XOR
    debug.xori := io.inst === XORI
    debug.sllv := io.inst === SLLV
    debug.sll := io.inst === SLL
    debug.srav := io.inst === SRAV
    debug.sra := io.inst === SRA
    debug.srlv := io.inst === SRLV
    debug.srl := io.inst === SRL
    debug.beq := io.inst === BEQ
    debug.bne := io.inst === BNE
    debug.bgez := io.inst === BGEZ
    debug.bgtz := io.inst === BGTZ
    debug.blez := io.inst === BLEZ
    debug.bltz := io.inst === BLTZ
    debug.bgezal := io.inst === BGEZAL
    debug.bltzal := io.inst === BLTZAL
    debug.j := io.inst === J
    debug.jal := io.inst === JAL
    debug.jr := io.inst === JR
    debug.jalr := io.inst === JALR
    debug.syscall := io.inst === SYSCALL
    debug.break := io.inst === BREAK
    debug.eret := io.inst === ERET
    debug.mfc0 := io.inst === MFC0
    debug.mtc0 := io.inst === MTC0
    debug.lb := io.inst === LB
    debug.lh := io.inst === LH
    debug.lw := io.inst === LW
    debug.lbu := io.inst === LBU
    debug.lhu := io.inst === LHU
    debug.sb := io.inst === SB
    debug.sh := io.inst === SH
    debug.sw := io.inst === SW
    debug.tlbp := io.inst === TLBP
    debug.tlbwi := io.inst === TLBWI
    debug.tlbr := io.inst === TLBR
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