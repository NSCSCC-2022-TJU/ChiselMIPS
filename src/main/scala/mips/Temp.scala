package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config._

class Temp() extends Module {
  val io = IO(new Bundle() {
    val in = Input(UInt(32.W))
    val out = Output(UInt(32.W))
  })

  val config = new Config


}

object LookupTree {
  def apply[T <: Data](key: UInt, mapping: Iterable[(UInt, T)]): T =
    Mux1H(mapping.map(p => (p._1 === key, p._2)))
}
object Temp extends App {
  println(getVerilogString(new Temp()))
  // (new ChiselStage).emitVerilog(new Temp(), Array("--target-dir", "output/"))
}
