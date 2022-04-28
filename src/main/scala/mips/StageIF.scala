package mips

import chisel3._
import chisel3.util._
import chisel3.stage.ChiselStage
import config.Config._
import config._

class IF2ID_IO(implicit config: Config) extends Bundle {
  val pc          = outputUInt(machine_width)
  val pc4         = outputUInt(machine_width)
  val jtsel       = Input(JTSel())
  val jumpAddr0   = inputUInt(machine_width)
  val jumpAddr1   = inputUInt(machine_width)
  val jumpAddr2   = inputUInt(machine_width)
  val excCode     = outputUInt(excCode_width)
  val refetchFlag = if (config.hasTLB) Some(inputBool) else None
}

class IF2Cache_IO extends Bundle {
  val dataOK     = inputBool
  val instReq    = outputBool
  val instAddrOK = inputBool
}

class StageIFIO(implicit config: Config) extends PipelineIO {
  val cp0io   = Flipped(new CP02IF_IO)
  val idio    = new IF2ID_IO
  val cacheio = new IF2Cache_IO
  val mmuio   = Flipped(new MMU2IF_IO())
}

class StageIF(implicit config: Config) extends Module {
  // IO
  val io      = IO(new StageIFIO())
  val flushio = io.flushio
  val scuio   = io.scuio
  val cp0io   = io.cp0io
  val idio    = io.idio
  val cacheio = io.cacheio
  val mmuio   = io.mmuio

  val flush = flushio.flush
  val stall = scuio.stall

  // 流水线寄存器 pipeline register
  val pc        = Reg(UInt(machine_width.W))
  val ce        = RegInit(0.B)
  val flushTemp = RegInit(0.B)

  val pcPlus4 = wireUInt(machine_width)
  pcPlus4 := pc + 4.U
  val pcNext = wireUInt(machine_width)
  pcNext := MyMux(idio.jtsel, Array(
    JTSel.PC4 -> pcPlus4,
    JTSel.Addr0 -> idio.jumpAddr0,
    JTSel.Addr1 -> idio.jumpAddr1,
    JTSel.Addr2 -> idio.jumpAddr2
  ))

  ce := true.B

  mmuio.addr := MyMuxWithPriority(pcNext, Array(
    !ce -> config.pcInit,
    flush -> cp0io.excAddr,
    flushTemp -> pc
  ))

  when(flush && stall(0)) {
    flushTemp := true.B
  }.elsewhen(cacheio.instAddrOK) {
    flushTemp := false.B
  }

  when(!ce) {
    pc := config.pcInit
  }.otherwise {
    when(flush) {
      pc := cp0io.excAddr
    }.elsewhen(flushTemp) {
      pc := pc
    }.elsewhen(stall(0) === false.B) {
      pc := pcNext
    }
  }

  scuio.stallReq := flushTemp || !(cacheio.instAddrOK && cacheio.dataOK)

  val wordAligned = mmuio.addr(1, 0) === 0.U

  val preExcCode = SoftMuxByConfig(
    config.hasTLB,
    MuxCase(excNone, Seq(
      (!wordAligned) -> excAdel,
      (!mmuio.found.get && mmuio.mapped.get) -> excTlbRefillL,
      (!mmuio.v.get && mmuio.mapped.get) -> excTlbInvalidL,
    )),
    Mux(!wordAligned, excAdel, excNone)
  )
  val excCodeReg = Reg(UInt(excCode_width.W))

  when(reset.asBool) {
    excCodeReg := excNone
  }.otherwise {
    when(flush) {
      excCodeReg := preExcCode
    }.elsewhen(flushTemp) {
      excCodeReg := excCodeReg
    }.elsewhen(stall(0) === false.B) {
      excCodeReg := preExcCode
    }
  }

  idio.excCode := excCodeReg

  idio.pc := pc
  idio.pc4 := pcPlus4

  cacheio.instReq := MyMuxWithPriority(true.B,
    if (config.hasTLB) {
      Array(
        (flushTemp && cacheio.instAddrOK) -> true.B,
        stall(0) -> false.B,
        idio.refetchFlag.get -> false.B,
        (preExcCode =/= excNone) -> false.B
      )
    } else {
      Array(
        (flushTemp && cacheio.instAddrOK) -> true.B,
        stall(0) -> false.B,
        (preExcCode =/= excNone) -> false.B
      )
    }
  )

}

object Stage_IF extends App {
  implicit val config = new Config
  println(getVerilogString(new StageIF()))
  // (new ChiselStage).emitVerilog(new Stage_IF(), Array("--target-dir", "output/"))
}
