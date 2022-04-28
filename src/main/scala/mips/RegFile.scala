package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config.Config._
import config._

class RegFileIO() extends Bundle {
  val raddr1 = inputUInt(regAddr_width)
  val raddr2 = inputUInt(regAddr_width)
  val rdata1 = outputUInt(machine_width)
  val rdata2 = outputUInt(machine_width)
  val wen    = inputBool
  val waddr  = inputUInt(regAddr_width)
  val wdata  = inputUInt(machine_width)
}

class RegFile() extends Module {
  val io   = IO(new RegFileIO())
  val regs = Mem(32, UInt(machine_width.W))

  //  io.rdata1 := Mux(io.raddr1.orR, regs(io.raddr1), 0.U)
  //  io.rdata2 := Mux(io.raddr2.orR, regs(io.raddr2), 0.U)
  io.rdata1 := MyMuxWithPriority(regs(io.raddr1), Array(
    (io.raddr1 === 0.U) -> nopWidth32,
    (io.wen && (io.waddr === io.raddr1)) -> io.wdata,
  ))
  io.rdata2 := MyMuxWithPriority(regs(io.raddr2), Array(
    (io.raddr2 === 0.U) -> nopWidth32,
    (io.wen && (io.waddr === io.raddr2)) -> io.wdata,
  ))

  when(io.wen & io.waddr.orR) {
    regs(io.waddr) := io.wdata
  }
}

object RegFile extends App {
  implicit val config = new Config
  println(getVerilogString(new RegFile()))
  // (new ChiselStage).emitVerilog(new StageEXE(), Array("--target-dir", "output/"))
}