package mips.config

import chisel3._
import chisel3.experimental.ChiselEnum

// 变量名尽量具有全局唯一性，以免导入多个Enum后变量名冲突，eg：“none”
object ALUOp extends ChiselEnum {
  // 从0开始编码 encode from 0
  val
  UNKNOWN,
  ADD,
  ADDU,
  SUB,
  SUBU,
  SLL,
  SRL,
  SRA,
  SLT,
  SLTU,
  XOR,
  OR,
  AND,
  LUI,
  NOR,
  MUL,
  MULT,
  MULTU,
  DIV,
  DIVU,
  MFHI,
  MFLO,
  MTHI,
  MTLO,
  J,
  JAL,
  JR,
  JALR,
  BEQ,
  BNE,
  BLEZ,
  BGTZ,
  BLTZ,
  BGEZ,
  BGEZAL,
  BLTZAL,
  SW,
  SH,
  SB,
  LW,
  LH,
  LHU,
  LB,
  LBU,
  MTC0,
  MFC0,
  SYSCALL,
  ERET,
  BREAK,
  TLBP,
  TLBR,
  TLBWI = Value
}

object ALUType extends ChiselEnum {
  val None, Arith, Logic, Shift, Mul, Mov, JAL,TLBP = Value
}

object JTSel extends ChiselEnum {
  val PC4, Addr0, Addr1, Addr2 = Value
}

object TlbOp extends ChiselEnum {
  val tlbNone, tlbwi, tlbr, tlbp = Value
}