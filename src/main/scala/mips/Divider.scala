package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

import scala.collection.immutable.Nil

class DividerIO() extends Bundle {
  val divideResult = outputUInt(machine_width * 2)
  val signed       = inputBool
  val src1         = inputUInt(machine_width)
  val src2         = inputUInt(machine_width)
  val ready        = outputBool
  val valid        = inputBool
}

// 两位试商法，16个计算周期
class Divider() extends Module {
  val io = IO(new DividerIO)

  val divTemp   = Wire(UInt(35.W))
  val divTemp0  = Wire(UInt(35.W))
  val divTemp1  = Wire(UInt(35.W))
  val divTemp2  = Wire(UInt(35.W))
  val divTemp3  = Wire(UInt(35.W))
  val mulCnt    = MuxCase("b01".U, Array(
    (divTemp3(34) === 0.U) -> "b11".U,
    (divTemp2(34) === 0.U) -> "b10".U,
  ))
  val regBundle = new Bundle() {
    val cnt      = UInt(6.W)
    val dividend = UInt(66.W)
    val state    = UInt(2.W)
    val divisor  = UInt(34.W)
    val ready    = Bool()
    val divRes   = UInt((machine_width * 2).W)
  }
  val reg       = Reg(regBundle)

  val cnt      = reg.cnt
  val dividend = reg.dividend
  val state    = reg.state
  val divisor  = reg.divisor
  val ready    = reg.ready
  val divRes   = reg.divRes

  val tempOp1 = Wire(UInt(32.W))
  val tempOp2 = Wire(UInt(32.W))
  tempOp1 := DontCare
  tempOp2 := DontCare

  val divisorTemp = tempOp2
  val divisor2    = (divisorTemp << 1.U).asTypeOf(UInt(34.W))
  val divisor3    = (divisor2 + divisor).asTypeOf(UInt(34.W))
  divTemp0 := Cat(0.U(1.W), dividend(63, 32)) - Cat(0.U(1.W), 0.U(machine_width.W))
  divTemp1 := Cat(0.U(1.W), dividend(63, 32)) - Cat(0.U(1.W), divisor)
  divTemp2 := Cat(0.U(1.W), dividend(63, 32)) - Cat(0.U(1.W), divisor2)
  divTemp3 := Cat(0.U(1.W), dividend(63, 32)) - Cat(0.U(1.W), divisor3)
  divTemp := MuxCase(divTemp1, Array(
    (divTemp3(34) === 0.U) -> divTemp3,
    (divTemp2(34) === 0.U) -> divTemp2,
  ))
  val divStart                                       = io.valid && !ready
  val divFree :: divByZero :: divOn :: divEnd :: Nil = Enum(4)

  when(reset.asBool) {
    state := divFree
    ready := false.B
    divRes := 0.U
  }.otherwise {
    switch(state) {
      is(divFree) {
        when(divStart) {
          when(io.src2 === 0.U) {
            state := divByZero
          }.otherwise {
            state := divOn
            cnt := 0.U
            when(io.signed) {
              when(io.src1(31) === 1.U) {
                tempOp1 := (~io.src1).asUInt + 1.U
              }.otherwise {
                tempOp1 := io.src1
              }
              when(io.src2(31) === 1.U) {
                tempOp2 := (~io.src2).asUInt + 1.U
              }.otherwise {
                tempOp2 := io.src2
              }
            }.otherwise {
              tempOp1 := io.src1
              tempOp2 := io.src2
            }
            dividend := Cat(Seq(0.U(machine_width.W), tempOp1))
            divisor := tempOp2
          }
        }.otherwise {
          ready := false.B
          divRes := 0.U
        }
      }
      is(divByZero) {
        dividend := 0.U((machine_width * 2).W)
        state := divEnd
      }
      is(divOn) {
        when(cnt =/= "b100010".U) {
          when(divTemp(34) === 1.U) {
            dividend := Cat(dividend(63, 0), 0.U(2.W))
          }.otherwise {
            dividend := Cat(Seq(divTemp(31, 0), dividend(31, 0), mulCnt))
          }
          cnt := cnt + 2.U
        }.otherwise {
          when(io.signed) {
            val cond1 = (io.src1(31) ^ io.src2(31)) === 1.U
            val cond2 = (io.src1(31) ^ dividend(65)) === 1.U
            when(cond1 && !cond2){
              dividend := Cat(dividend(65, 32), (~dividend(31, 0)).asUInt + 1.U)
            }.elsewhen(!cond1 && cond2){
              dividend := Cat((~dividend(65, 34)).asUInt + 1.U, dividend(33, 0))
            }.elsewhen(cond1 && cond2){
              dividend := Cat(Seq((~dividend(65, 34)).asUInt + 1.U,dividend(33, 32),(~dividend(31, 0)).asUInt + 1.U))
            }
          }
          state := divEnd
          cnt := 0.U
        }
      }
      is(divEnd) {
        divRes := Cat(dividend(65, 34), dividend(31, 0))
        ready := true.B
        when(divStart === false.B) {
          state := divFree
          ready := false.B
          divRes := 0.U
        }
      }
    }
  }


  io.ready := ready
  io.divideResult := divRes

}

object Divider extends App {
  implicit val config = new Config
  println(getVerilogString(new Divider()))
  // (new ChiselStage).emitVerilog(new Divider(), Array("--target-dir", "output/"))
}
