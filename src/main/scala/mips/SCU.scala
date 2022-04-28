package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._
import config.Config._

class Stage2SCU_IO extends Bundle {
  val stall = inputUInt(stall_width)
  val stallReq = outputBool
}

class SCUIO() extends Bundle {
  val stall = outputUInt(stall_width)
  val stallReqIF = inputBool
  val stallReqID = inputBool
  val stallReqEXE = inputBool
  val stallReqMEM = inputBool
}

class SCU() extends Module {
  val io = IO(new SCUIO)

  when(io.stallReqMEM){
    io.stall := "b011111".U
  }.elsewhen(io.stallReqEXE){
    io.stall := "b001111".U
  }.elsewhen(io.stallReqID){
    io.stall := "b000111".U
  }.elsewhen(io.stallReqIF){
    io.stall := "b000111".U
  }.otherwise{
    io.stall := "b000000".U
  }
}

//object SCU extends App {
//  implicit val config = new Config
//  println(getVerilogString(new SCU()))
//  // (new ChiselStage).emitVerilog(new SCU(), Array("--target-dir", "output/"))
//}
