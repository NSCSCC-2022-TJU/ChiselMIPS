package mips.config

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import Config._

/**
 * 继承后需要重写Stage信息，暂时没有想到一个更好的实现方案。
 */
class PipelineBundle extends Bundle {
  val isStageIF  = false
  val isStageID  = false
  val isStageEXE = false
  val isStageMEM = false
  val isStageWB  = false
  val seq        = Seq(isStageIF, isStageID, isStageEXE, isStageMEM, isStageWB)
  require(seq.filter(_ == true).length == 1, "[PipelineBundle]需要重写阶段信息 have to override Stage information")




}


