package Pipeline

import chisel3._
import chisel3.util._
import Define._

class CP0 extends Module{
	val io = IO(new Bundle{
		val index		= Input(UInt(5.W))
		val datain	= Input(UInt(32.W))
		val CP0ctr 	= Input(UInt(4.W))
		val dataout	= Output(UInt(32.W))

		val BD = Input(UInt(1.W))
		val Status = Input(UInt(32.W))
		val ExcCode = Input(UInt(5.W))
		val BadVAddr = Input(UInt(32.W))
		val EPC = Input(UInt(32.W))
		val IsExc = Input(Bool())

		val ExcHapp = Output(Bool())
		val IF_ID_Flush = Output(UInt(1.W))
		val ID_EX_Flush = Output(UInt(1.W))
		val EX_MEM_Flush = Output(UInt(1.W))
		val MEM_WB_Flush = Output(UInt(1.W))
		})

	val RegFile = Mem(32, UInt(32.W))//寄存器堆

	def UnuseOutput() ={
		io.IF_ID_Flush := false.B
		io.ID_EX_Flush := false.B
		io.EX_MEM_Flush := false.B
		io.MEM_WB_Flush := false.B
		io.ExcHapp := false.B
	}

	when(io.CP0ctr === MT_CO){
		RegFile(io.index) := io.datain
		io.dataout := 0.U
		UnuseOutput()


	}.elsewhen(io.CP0ctr === MF_CO){
		io.dataout := RegFile(io.index)
		UnuseOutput()


		//ERET指令需要判断是否产生异常
	}.elsewhen(io.CP0ctr === Eret){
		io.dataout := RegFile(14.U)

		when(io.dataout > 0x00ffffff.U){
			val cause = Cat(io.BD,Fill(24,0.U(1.W)),io.ExcCode,0.U(2.W))
			RegFile(13.U) := cause
			RegFile(14.U) := io.EPC
			RegFile(8.U) := RegFile(14.U)//BadVAddr
			RegFile(12.U) := io.Status//Status

			io.IF_ID_Flush := true.B
			io.ID_EX_Flush := true.B
			io.EX_MEM_Flush := true.B
			io.MEM_WB_Flush := false.B
			io.ExcHapp := true.B
		}.otherwise({
			UnuseOutput()
		})




		//异常处理
	}.otherwise({
		when(io.IsExc){
			val cause = Cat(io.BD,Fill(24,0.U(1.W)),io.ExcCode,0.U(2.W))
			RegFile(13.U) := cause
			RegFile(14.U) := io.EPC
			RegFile(8.U) := io.BadVAddr//BadVAddr
			RegFile(12.U) := io.Status//Status

			io.dataout := 0.U
			io.IF_ID_Flush := true.B
			io.ID_EX_Flush := true.B
			io.EX_MEM_Flush := true.B
			io.MEM_WB_Flush := false.B
			io.ExcHapp := io.IsExc
		}.otherwise({
			io.dataout := 0.U
			UnuseOutput()
		})
	})
	/*
	printf("EPC=0x%x,cause=0x%x,dataout=0x%x,Flush=%d,%d,%d,%d",RegFile(14.U),RegFile(13.U),io.dataout,io.IF_ID_Flush,io.ID_EX_Flush,io.EX_MEM_Flush,io.MEM_WB_Flush)
	*/
}