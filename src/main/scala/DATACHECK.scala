package Pipeline

import chisel3._


class DataCheck extends Module{
	val io = IO(new Bundle{
		val ID_EX_Rs		= Input(UInt(5.W))
		val ID_EX_Rt 	= Input(UInt(5.W))
		val Ex_Mem_Rd 	= Input(UInt(5.W))
		val Mem_WB_Rd 	= Input(UInt(5.W))
		val Ex_Mem_RegWr = Input(Bool())
		val Mem_WB_RegWr = Input(Bool())
		val ForwardA = Output(UInt(2.W))
		val ForwardB = Output(UInt(2.W))
		})

		when(io.Ex_Mem_RegWr && io.Ex_Mem_Rd =/= 0.U && io.Ex_Mem_Rd === io.ID_EX_Rs){
			io.ForwardA := "b10".U(2.W)
		}.elsewhen(io.Mem_WB_RegWr && io.Mem_WB_Rd =/= 0.U && io.Mem_WB_Rd === io.ID_EX_Rs){
			io.ForwardA := "b01".U(2.W)
		}.otherwise({
			io.ForwardA := "b00".U(2.W)
		})

		when(io.Ex_Mem_RegWr && io.Ex_Mem_Rd =/= 0.U && io.Ex_Mem_Rd === io.ID_EX_Rt){
			io.ForwardB := "b10".U(2.W)
		}.elsewhen(io.Mem_WB_RegWr && io.Mem_WB_Rd =/= 0.U && io.Mem_WB_Rd === io.ID_EX_Rt){
			io.ForwardB := "b01".U(2.W)
		}.otherwise({
			io.ForwardB := "b00".U(2.W)
		})
}