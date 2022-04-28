package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config.Config._
import config._

class ALUIO() extends Bundle {
  val src1     = inputUInt(machine_width)
  val src2     = inputUInt(machine_width)
  val aluOp    = Input(ALUOp())
  val outLogic = outputUInt(machine_width)
  val outArith = outputUInt(machine_width)
  val outShift = outputUInt(machine_width)
  val outDiv   = outputUInt(machine_width * 2)
  val outMul   = outputUInt(machine_width * 2)
  val ov       = outputBool
  val stallReq = outputBool
}

class ALU() extends Module {
  val io = IO(new ALUIO)
  Seq(io).foreach(_ := DontCare)
  val src1     = io.src1
  val src2     = io.src2
  val aluOp    = io.aluOp
  val outLogic = io.outLogic
  val outArith = io.outArith
  val outShift = io.outShift
  val outDiv   = io.outDiv
  val outMul   = io.outMul
  val ov       = io.ov
  val stallReq = io.stallReq


  import ALUOp._

  outArith := MyMuxWithPriority((src1 + src2),Array(
    aluOp.isOneOf(ALUOp.ADD,ALUOp.ADDU) -> (src1 + src2),
    aluOp.isOneOf(ALUOp.SUB,ALUOp.SUBU) -> (src1 - src2),
    aluOp.isOneOf(ALUOp.SLT) -> (src1.asSInt < src2.asSInt).asUInt,
    aluOp.isOneOf(ALUOp.SLTU) -> (src1.asUInt < src2.asUInt),
  ))


  ov := MuxLookup(aluOp.asUInt, false.B, Array(
    SUB.asUInt -> ((src1(31) && !src2(31) && !outArith(31))
      || (!src1(31) && src2(31) && outArith(31))).asBool,
    ADD.asUInt -> ((src1(31) && src2(31) && !outArith(31))
      || (!src1(31) && !src2(31) && outArith(31))).asBool
  ))

  outLogic := MyMux(aluOp, Array(
    XOR -> (src1 ^ src2),
    OR -> (src1 | src2),
    AND -> (src1 & src2),
    NOR -> ~(src1 | src2),
    LUI -> src2,
  ))

  outShift := MyMux(aluOp, Array(
    SLL -> (src2.asUInt << src1(4, 0)),
    SRL -> (src2.asUInt >> src1(4, 0)).asUInt,
    SRA -> (src2.asSInt >> src1(4, 0)).asUInt,
  ))


  outMul := Mux(aluOp.isOneOf(ALUOp.MULTU),
    (src1 * src2),
    (src1.asSInt * src2.asSInt).asUInt)

  val divider = Module(new Divider())
  divider.io.signed := aluOp.isOneOf(ALUOp.DIV)
  divider.io.src1 := src1
  divider.io.src2 := src2
  divider.io.valid := aluOp.isOneOf(ALUOp.DIV,ALUOp.DIVU)

  outDiv := divider.io.divideResult
  stallReq := aluOp.isOneOf(ALUOp.DIV,ALUOp.DIVU) && !divider.io.ready
}

object ALU extends App {
  println(getVerilogString(new ALU()))
  // (new ChiselStage).emitVerilog(new Alu(), Array("--target-dir", "output/"))
}
