package mips.config

import chisel3._
import Chisel.BitPat

object Instructions {

  def NOP = BitPat("b00000000000000000000000000000000")

  //14条算数

  def ADD = BitPat("b000000???????????????00000100000")

  def ADDI = BitPat("b001000??????????????????????????")

  def ADDU = BitPat("b000000???????????????00000100001")

  def ADDIU = BitPat("b001001??????????????????????????")

  def SUB = BitPat("b000000???????????????00000100010")

  def SUBU = BitPat("b000000???????????????00000100011")

  def SLT = BitPat("b000000???????????????00000101010")

  def SLTI = BitPat("b001010??????????????????????????")

  def SLTU = BitPat("b000000???????????????00000101011")

  def SLTIU = BitPat("b001011??????????????????????????")


  def DIV = BitPat("b000000??????????0000000000011010")

  def DIVU = BitPat("b000000??????????0000000000011011")

  def MULT = BitPat("b000000??????????0000000000011000")

  def MULTU = BitPat("b000000??????????0000000000011001")

  // 8条逻辑
  def AND = BitPat("b000000???????????????00000100100")

  def ANDI = BitPat("b001100??????????????????????????")

  def LUI = BitPat("b00111100000?????????????????????")

  def NOR = BitPat("b000000???????????????00000100111")

  def OR = BitPat("b000000???????????????00000100101")

  def ORI = BitPat("b001101??????????????????????????")

  def XOR = BitPat("b000000???????????????00000100110")

  def XORI = BitPat("b001110??????????????????????????")

  // 6条移位指令
  def SLLV = BitPat("b000000???????????????00000000100")

  def SLL = BitPat("b00000000000???????????????000000")

  def SRAV = BitPat("b000000???????????????00000000111")

  def SRA = BitPat("b00000000000???????????????000011")

  def SRLV = BitPat("b000000???????????????00000000110")

  def SRL = BitPat("b00000000000???????????????000010")

  // 12条分支跳转
  def BEQ = BitPat("b000100??????????????????????????")

  def BNE = BitPat("b000101??????????????????????????")

  def BGEZ = BitPat("b000001?????00001????????????????")

  def BGTZ = BitPat("b000111?????00000????????????????")

  def BLEZ = BitPat("b000110?????00000????????????????")

  def BLTZ = BitPat("b000001?????00000????????????????")

  def BGEZAL = BitPat("b000001?????10001????????????????")

  def BLTZAL = BitPat("b000001?????10000????????????????")

  def J = BitPat("b000010??????????????????????????")

  def JAL = BitPat("b000011??????????????????????????")

  def JR = BitPat("b000000?????000000000000000001000")

  def JALR = BitPat("b000000?????00000?????00000001001") //rd=31(implied)?????or11111

  // 4条数据移动
  def MFHI = BitPat("b0000000000000000?????00000010000")

  def MFLO = BitPat("b0000000000000000?????00000010010")

  def MTHI = BitPat("b000000?????000000000000000010001")

  def MTLO = BitPat("b000000?????000000000000000010011")

  def MUL = BitPat("b011100???????????????00000000010")

  // 2 条自陷
  def BREAK = BitPat("b000000????????????????????001101")

  def SYSCALL = BitPat("b000000????????????????????001100")

  // 8条访存
  def LB = BitPat("b100000??????????????????????????")

  def LBU = BitPat("b100100??????????????????????????")

  def LH = BitPat("b100001??????????????????????????")

  def LHU = BitPat("b100101??????????????????????????")

  def LW = BitPat("b100011??????????????????????????")

  def SB = BitPat("b101000??????????????????????????")

  def SH = BitPat("b101001??????????????????????????")

  def SW = BitPat("b101011??????????????????????????")

  // 3条特权
  def ERET = BitPat("b01000010000000000000000000011000")

  def MFC0 = BitPat("b01000000000??????????00000000???")

  def MTC0 = BitPat("b01000000100??????????00000000???")

  // 3条TLB
  def TLBP = BitPat("b010000_1_000_0000_0000_0000_0000_001000")

  def TLBR = BitPat("b010000_1_000_0000_0000_0000_0000_000001")

  def TLBWI = BitPat("b010000_1_000_0000_0000_0000_0000_000010")


  //                              aluop       alutype      rreg1    rreg2   wreg    whilo   shift     immsel    rtsel    sext      mreg
  val decode_default = List(ALUOp.UNKNOWN, ALUType.Arith, false.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B)
  //                aluop       alutype     rreg1    rreg2  wreg    whilo    shift   immsel    rtsel    sext        mreg
  val decode_table   = Array(
    ADD -> List(ALUOp.ADD, ALUType.Arith, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    ADDI -> List(ALUOp.ADD, ALUType.Arith, true.B, false.B, true.B, false.B, false.B, true.B, true.B, true.B, false.B),
    ADDU -> List(ALUOp.ADDU, ALUType.Arith, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    ADDIU -> List(ALUOp.ADDU, ALUType.Arith, true.B, false.B, true.B, false.B, false.B, true.B, true.B, true.B, false.B),
    SUB -> List(ALUOp.SUB, ALUType.Arith, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    SUBU -> List(ALUOp.SUBU, ALUType.Arith, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    SLT -> List(ALUOp.SLT, ALUType.Arith, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    SLTI -> List(ALUOp.SLT, ALUType.Arith, true.B, false.B, true.B, false.B, false.B, true.B, true.B, true.B, false.B),
    SLTU -> List(ALUOp.SLTU, ALUType.Arith, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    SLTIU -> List(ALUOp.SLTU, ALUType.Arith, true.B, false.B, true.B, false.B, false.B, true.B, true.B, true.B, false.B),
    MUL -> List(ALUOp.MUL, ALUType.Mul, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    DIV -> List(ALUOp.DIV, ALUType.None, true.B, true.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B),
    DIVU -> List(ALUOp.DIVU, ALUType.None, true.B, true.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B),
    MULT -> List(ALUOp.MULT, ALUType.None, true.B, true.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B),
    MULTU -> List(ALUOp.MULTU, ALUType.None, true.B, true.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B),
    MFHI -> List(ALUOp.MFHI, ALUType.Mov, false.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    MFLO -> List(ALUOp.MFLO, ALUType.Mov, false.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    MTHI -> List(ALUOp.MTHI, ALUType.None, true.B, false.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B),
    MTLO -> List(ALUOp.MTLO, ALUType.None, true.B, false.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B),
    AND -> List(ALUOp.AND, ALUType.Logic, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    ANDI -> List(ALUOp.AND, ALUType.Logic, true.B, false.B, true.B, false.B, false.B, true.B, true.B, false.B, false.B),
    LUI -> List(ALUOp.LUI, ALUType.Logic, false.B, false.B, true.B, false.B, false.B, true.B, true.B, false.B, false.B),
    NOR -> List(ALUOp.NOR, ALUType.Logic, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    OR -> List(ALUOp.OR, ALUType.Logic, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    ORI -> List(ALUOp.OR, ALUType.Logic, true.B, false.B, true.B, false.B, false.B, true.B, true.B, false.B, false.B),
    XOR -> List(ALUOp.XOR, ALUType.Logic, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    XORI -> List(ALUOp.XOR, ALUType.Logic, true.B, false.B, true.B, false.B, false.B, true.B, true.B, false.B, false.B),
    SLLV -> List(ALUOp.SLL, ALUType.Shift, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    SLL -> List(ALUOp.SLL, ALUType.Shift, false.B, true.B, true.B, false.B, true.B, false.B, false.B, false.B, false.B),
    SRAV -> List(ALUOp.SRA, ALUType.Shift, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    SRA -> List(ALUOp.SRA, ALUType.Shift, false.B, true.B, true.B, false.B, true.B, false.B, false.B, false.B, false.B),
    SRLV -> List(ALUOp.SRL, ALUType.Shift, true.B, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    SRL -> List(ALUOp.SRL, ALUType.Shift, false.B, true.B, true.B, false.B, true.B, false.B, false.B, false.B, false.B),
    BEQ -> List(ALUOp.BEQ, ALUType.None, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    BNE -> List(ALUOp.BNE, ALUType.None, true.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    BGEZ -> List(ALUOp.BGEZ, ALUType.None, true.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    BGTZ -> List(ALUOp.BGTZ, ALUType.None, true.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    BLEZ -> List(ALUOp.BLEZ, ALUType.None, true.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    BLTZ -> List(ALUOp.BLTZ, ALUType.None, true.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    BGEZAL -> List(ALUOp.BGEZAL, ALUType.JAL, true.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    BLTZAL -> List(ALUOp.BLTZAL, ALUType.JAL, true.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    J -> List(ALUOp.J, ALUType.None, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    JAL -> List(ALUOp.JAL, ALUType.JAL, false.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    JR -> List(ALUOp.JR, ALUType.None, true.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    JALR -> List(ALUOp.JALR, ALUType.JAL, true.B, false.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B),
    SYSCALL -> List(ALUOp.SYSCALL, ALUType.None, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    BREAK -> List(ALUOp.BREAK, ALUType.None, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    ERET -> List(ALUOp.ERET, ALUType.None, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    MFC0 -> List(ALUOp.MFC0, ALUType.Mov, false.B, false.B, true.B, false.B, false.B, false.B, true.B, false.B, false.B),
    MTC0 -> List(ALUOp.MTC0, ALUType.None, false.B, true.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    LB -> List(ALUOp.LB, ALUType.Arith, true.B, false.B, true.B, false.B, false.B, true.B, true.B, true.B, true.B),
    LH -> List(ALUOp.LH, ALUType.Arith, true.B, false.B, true.B, false.B, false.B, true.B, true.B, true.B, true.B),
    LW -> List(ALUOp.LW, ALUType.Arith, true.B, false.B, true.B, false.B, false.B, true.B, true.B, true.B, true.B),
    LBU -> List(ALUOp.LBU, ALUType.Arith, true.B, false.B, true.B, false.B, false.B, true.B, true.B, true.B, true.B),
    LHU -> List(ALUOp.LHU, ALUType.Arith, true.B, false.B, true.B, false.B, false.B, true.B, true.B, true.B, true.B),
    SB -> List(ALUOp.SB, ALUType.Arith, true.B, true.B, false.B, false.B, false.B, true.B, false.B, true.B, false.B),
    SH -> List(ALUOp.SH, ALUType.Arith, true.B, true.B, false.B, false.B, false.B, true.B, false.B, true.B, false.B),
    SW -> List(ALUOp.SW, ALUType.Arith, true.B, true.B, false.B, false.B, false.B, true.B, false.B, true.B, false.B),
    TLBP -> List(ALUOp.TLBP, ALUType.TLBP, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    TLBWI -> List(ALUOp.TLBWI, ALUType.None, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
    TLBR -> List(ALUOp.TLBR, ALUType.None, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B, false.B),
  )
}
