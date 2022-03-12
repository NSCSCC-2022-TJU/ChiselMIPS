package Pipeline

import chisel3._
import Define._


class CMP extends Module{
    val io = IO(new Bundle{
        val in1 = Input(UInt(32.W))
        val in2 	= Input(UInt(32.W))
		val CMPctr 	= Input(UInt(4.W))
		val CMPout	= Output(Bool())
    })

    when(io.CMPctr===CMP_equ){//判断是否相等
		io.CMPout := io.in1===io.in2


	}.elsewhen(io.CMPctr===CMP_neq){//判断是否不等
		io.CMPout := io.in1=/=io.in2


	}.elsewhen(io.CMPctr===CMP_slt){//有符号小于
		io.CMPout := io.in1.asSInt()<io.in2.asSInt()


	}.elsewhen(io.CMPctr===CMP_sltu){//无符号小于
		io.CMPout := io.in1<io.in2


  }.elsewhen(io.CMPctr===CMP_bgez){//大于等于0
    io.CMPout := io.in1.asSInt() >= 0.S


  }.elsewhen(io.CMPctr===CMP_bgtz){//大于0
    io.CMPout := io.in1.asSInt() > 0.S


  }.elsewhen(io.CMPctr===CMP_blez){//小于等于0
    io.CMPout := io.in1.asSInt() <= 0.S


  }.elsewhen(io.CMPctr===CMP_bltz){//小于0
    io.CMPout := io.in1.asSInt() < 0.S


  }.otherwise({
		io.CMPout := 0.U
	})
}