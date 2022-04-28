package mips.config

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import Config._
import mips.{CP02Stage_IO, Stage2SCU_IO}


class PipelineIO() extends Bundle {
  val flushio = Flipped(new CP02Stage_IO)
  val scuio   = new Stage2SCU_IO
}

class ByPassIO(hasData: Boolean = true) extends Bundle {
  val wreg  = inputBool
  val waddr = inputUInt(regAddr_width)
  val wdata = if (hasData) Some(inputUInt(machine_width)) else None

  def hazard(raddr: UInt, rreg: Bool) = {
    wreg && waddr === raddr && rreg
  }

  def hazard(raddr: UInt) = {
    wreg && waddr === raddr
  }
}