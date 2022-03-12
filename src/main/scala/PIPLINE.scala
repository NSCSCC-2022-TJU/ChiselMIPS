package Pipeline

import chisel3._
import chisel3.util._
import Define._

class PipeIo extends Bundle() {

  val boot = Input(Bool())
  val test_wr = Input(Bool())
  val test_addr = Input(UInt(32.W))
  val test_inst = Input(UInt(32.W))
  val test_wr_dmm = Input(Bool())
  val test_addr_dmm = Input(UInt(32.W))
  val test_data = Input(UInt(32.W))
  val test_exc = Input(UInt(3.W))
  val Inst = Output(UInt(32.W))
  val imem_addr	= Output(UInt(32.W))
  val Branch = Output(Bool())
  val Bhappen = Output(Bool())
  val Bpredict = Output(Bool())
}

class Pipelines extends Module() {
  val io = IO(new PipeIo())
  //------------------Modules------------------//

  val imm = Mem(20000, UInt(32.W))//指令存储器
  val dmm = Mem(2000000, UInt(8.W))//以字节为一个单位的数据存储器
  val RegFile = Mem(32, UInt(32.W))//寄存器堆
  val dataCheck = Module(new DataCheck())//数据冒险单元
  val alu = Module(new ALU())//算数逻辑单元
  val cmp = Module(new CMP())//比较单元
  val cp0 = Module(new CP0())//协处理器

//------------------control------------------//

//Flush
  val IF_ID_Flush = Wire(Bool())
  val ID_EX_Flush = Wire(Bool())
  val EX_MEM_Flush = Wire(Bool())
  val MEM_WB_Flush = Wire(Bool())

  //wb
  val RegWr =   Wire(Bool())
  val RegSrc =  Wire(UInt(2.W))
  val AL =      Wire(Bool())

  //M
  val MemWr =   Wire(Bool())

  //ex
  val Exc =     Wire(UInt(2.W))
  val Jr =      Wire(Bool())
  val Shamt =   Wire(Bool())
  val RegDst =  Wire(Bool())
  val ALUctr =  Wire(UInt(6.W))
  val CMPctr =  Wire(UInt(4.W))
  val ALUsrc =  Wire(Bool())
  val ExtOp =   Wire(Bool())
  val Jump =    Wire(Bool())
  val BorJ =    Wire(Bool())

  //data
  val Rs =      Wire(UInt(5.W))
  val Rd =      Wire(UInt(5.W))
  val Rt =      Wire(UInt(5.W))
  val BusA =    Wire(UInt(32.W))
  val BusB =    Wire(UInt(32.W))
  val Imm26 =   Wire(UInt(26.W))
  val Imm32 =   Wire(UInt(32.W))
  val Memout =  Wire(UInt(32.W))
  val Memin =   Wire(UInt(32.W))
  val CMPout =  Wire(Bool())
  val nPC_sel = Wire(Bool())

  //------------------pc------------------//
  
  val pc_next = Wire(UInt(32.W))
  val pc_plus4= Wire(UInt(32.W))
  val pc_br 	= Wire(UInt(32.W))
  val pc_reg  = RegNext(next = pc_next, init=0.U(32.W))


  //----------------------------------------//
  //------------------boot------------------//
  //----------------------------------------//

  when(io.boot && io.test_wr) {
    imm((io.test_addr >> 2).asUInt()) := io.test_inst
  }

  when(io.boot && io.test_wr_dmm) {
    dmm(io.test_addr_dmm.asUInt()) := io.test_data(7,0)
    dmm(io.test_addr_dmm.asUInt() + 1.U) := io.test_data(15,8)
    dmm(io.test_addr_dmm.asUInt() + 2.U) := io.test_data(23,16)
    dmm(io.test_addr_dmm.asUInt() + 3.U) := io.test_data(31,24)

  }

  val clk_cnt = RegInit(0.U(32.W))
  clk_cnt := clk_cnt + 1.U

  //----------------------------------------//
  //------------------Registers-------------//
  //----------------------------------------//
  val IF_ID_ins =     RegInit(init = 0.U(32.W))
  val IF_ID_pcPlus4 = RegNext(next = Mux(IF_ID_Flush,0.U,pc_plus4))

  //WB
  val ID_EX_RegWr =   RegNext(next =Mux(ID_EX_Flush,false.B,RegWr))
  val ID_EX_RegSrc =  RegNext(next =Mux(ID_EX_Flush,Regsrc_X,RegSrc))
  val ID_EX_AL =      RegNext(next = Mux(ID_EX_Flush,false.B,AL))
  //M
  val ID_EX_MemWr =   RegNext(next =Mux(ID_EX_Flush,false.B,MemWr))
  //EX
  val ID_EX_Exc =     RegNext(next = Mux(ID_EX_Flush,EX_N,Exc))
  val ID_EX_Jr =      RegNext(next = Mux(ID_EX_Flush,false.B,Jr))
  val ID_EX_Shamt =   RegNext(next = Mux(ID_EX_Flush,false.B,Shamt))
  val ID_EX_RegDst =  RegNext(next = Mux(ID_EX_Flush,false.B,RegDst))
  val ID_EX_ALUctr=   RegNext(next = Mux(ID_EX_Flush,ALU_X,ALUctr))
  val ID_EX_ALUsrc=   RegNext(next = Mux(ID_EX_Flush,false.B,ALUsrc))
  val ID_EX_Jump=     RegNext(next = Mux(ID_EX_Flush,false.B,Jump))
  val ID_EX_BorJ=     RegNext(next = Mux(ID_EX_Flush,false.B,BorJ))
  val ID_EX_CMPctr=   RegNext(next = Mux(ID_EX_Flush,CMP_X,CMPctr))
  //DATA
  val ID_EX_BusA=     RegNext(next = BusA)
  val ID_EX_BusB=     RegNext(next = BusB)
  val ID_Ex_Rs=       RegNext(next = Rs)
  val ID_EX_Rd=       RegNext(next = Rd)
  val ID_EX_Rt=       RegNext(next = Rt)
  val ID_EX_pcPlus4=  RegNext(next = IF_ID_pcPlus4)
  val ID_EX_Imm26=    RegNext(next = Imm26)
  val ID_EX_Imm32=    RegNext(next = Imm32)


  //EX
  val EX_MEM_BorJ=    RegNext(next = Mux(EX_MEM_Flush,false.B,ID_EX_BorJ))
  //WB
  val EX_MEM_RegWr =  RegNext(next = Mux(EX_MEM_Flush,false.B,ID_EX_RegWr))
  val EX_MEM_RegSrc = RegNext(next = Mux(EX_MEM_Flush,false.B,ID_EX_RegSrc))
  val EX_MEM_AL =     RegNext(next = Mux(EX_MEM_Flush,false.B,ID_EX_AL))
  //M
  val EX_MEM_MemWr =  RegNext(next = Mux(EX_MEM_Flush,false.B,ID_EX_MemWr))
  //DATA
  val EX_MEM_pcAL=    RegNext(next = ID_EX_pcPlus4+4.U)
  val EX_MEM_ALUout=  RegNext(next = Mux(ID_EX_Jr,cp0.io.dataout,alu.io.alu_out))//JR用于CP0控制
  val EX_MEM_Rt=      RegNext(next = ID_EX_Rt)
  val EX_MEM_LSctr=   RegNext(next = ID_EX_CMPctr)
  val EX_MEM_CMPout=  RegNext(next = cmp.io.CMPout)
  val EX_MEM_dataIn=  RegNext(next = ID_EX_BusB)
  val EX_MEM_reg_index=RegNext(next = Mux(ID_EX_AL,31.U(5.W),Mux(ID_EX_RegDst, ID_EX_Rd, ID_EX_Rt)))//AL高时，直接写回31号寄存器


  //WB
  val MEM_WB_RegWr =  RegNext(next = Mux(MEM_WB_Flush,false.B,EX_MEM_RegWr))
  val MEM_WB_RegSrc = RegNext(next = Mux(MEM_WB_Flush,false.B,EX_MEM_RegSrc))
  val MEM_WB_AL =     RegNext(next = Mux(MEM_WB_Flush,false.B,EX_MEM_AL))
  //DATA
  val MEM_WB_pcAL=    RegNext(next = EX_MEM_pcAL)
  val MEM_WB_ALUout=  RegNext(next = EX_MEM_ALUout)
  val MEM_WB_CMPout=  RegNext(next = EX_MEM_CMPout)
  val MEM_WB_reg_index=RegNext(next = EX_MEM_reg_index)
  val MEM_WB_MemOut=  RegNext(next = Memout)


  //回写寄存器
  val Reg_WrData 	= Wire(UInt(32.W))
  val Reg_index = Wire(UInt(5.W))

  /*
  printf("Cyc=%d, pc=0x%x, Inst=0x%x\n",
    clk_cnt,
    io.imem_addr,
    IF_ID_ins
    )
  */
  
  //----------------------------------------//
  //---------------IF and ID----------------//
  //----------------------------------------//
  RegWr:=false.B
  MemWr:=false.B

  //IF_Flush
  when(io.boot) {
    IF_ID_ins := 0.U
  }.elsewhen(nPC_sel){//如果跳转，填充一个气泡
    IF_ID_ins := 0.U
  }.elsewhen(IF_ID_Flush){
    IF_ID_ins := 0.U
  }.otherwise({
    IF_ID_ins := imm((io.imem_addr >> 2).asUInt())
  })

  io.Inst := IF_ID_ins

  Rs := IF_ID_ins(25, 21)
  Rt := IF_ID_ins(20, 16)
  Rd := IF_ID_ins(15, 11)

  // Zero reg always zero
  RegFile(0) := 0.U

  // Read Register
  //解决了同时读写寄存器堆的潜在冒险
  when(MEM_WB_RegWr === true.B && Rs === Reg_index){
    BusA:=Reg_WrData
  }.otherwise({
    BusA := RegFile(Rs)
  })

  when(MEM_WB_RegWr === true.B && Rt === Reg_index){
    BusB:=Reg_WrData
  }.otherwise({
    BusB := RegFile(Rt)
  })

//printf("Rs=%d,BusA=0x%x",Rs,BusA)

  //----------------------------------------//
  //-----------------Decode-----------------//
  //----------------------------------------//

  //I表示有效信号
  //O表示无效信号
  //X表示无关信号
  val control =
    ListLookup(IF_ID_ins,
      List(        EX_res,    O,     O,     O,    CMP_X,      O,   Regsrc_X,   O,     O,       O,    O,      O,       O,     ALU_X),
            Array(/*| Exc  |Jr(exc)| AL | Shamt | CMPctr | RegDst | RegSrc | RegWr | MemWr | BorJ | Jump | ALUsrc | Extop | ALUctr |   */

      ADD    ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Addex),
      ADDIU  ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_Alu, I,     O,       O,    O,      I,       I,     ALU_Add),
      ADDU   ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Add),
      ADDI   ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_Alu, I,     O,       O,    O,      I,       I,     ALU_Addex),
      SUB    ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Subex),
      SUBU   ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Sub),
      SLT    ->List( EX_N,     O,     O,     O,    CMP_slt,    I,   Regsrc_CMP, I,     O,       O,    O,      O,       X,     ALU_X),
      SLTI   ->List( EX_N,     O,     O,     O,    CMP_slt,    O,   Regsrc_CMP, I,     O,       O,    O,      I,       I,     ALU_X),
      SLTU   ->List( EX_N,     O,     O,     O,    CMP_sltu,   I,   Regsrc_CMP, I,     O,       O,    O,      O,       X,     ALU_X),
      SLTIU  ->List( EX_N,     O,     O,     O,    CMP_sltu,   O,   Regsrc_CMP, I,     O,       O,    O,      I,       I,     ALU_X),
      MULT   ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_X,   I,     O,       O,    O,      O,       X,     ALU_Mult),
      MULTU  ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_X,   I,     O,       O,    O,      O,       X,     ALU_Multu),
      DIV    ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_X,   I,     O,       O,    O,      O,       X,     ALU_Div),
      DIVU   ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_X,   I,     O,       O,    O,      O,       X,     ALU_Divu),
      AND    ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_And),
      ANDI   ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_Alu, I,     O,       O,    O,      I,       O,     ALU_And),
      LUI    ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_X,   I,     O,       O,    O,      I,       O,     ALU_Lui),
      NOR    ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Nor),
      OR     ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Or),
      ORI    ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_Alu, I,     O,       O,    O,      I,       O,     ALU_Or),
      XOR    ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Xor),
      XORI   ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_Alu, I,     O,       O,    O,      I,       O,     ALU_Xor),
      SLL    ->List( EX_N,     O,     O,     I,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Sll),
      SLLV   ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Sllv),
      SRA    ->List( EX_N,     O,     O,     I,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Sra),
      SRAV   ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Srav),
      SRL    ->List( EX_N,     O,     O,     I,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Srl),
      SRLV   ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       X,     ALU_Srlv),
      BEQ    ->List( EX_N,     O,     O,     O,    CMP_equ,    O,   Regsrc_X,   O,     O,       I,    O,      O,       I,     ALU_X),
      BNE    ->List( EX_N,     O,     O,     O,    CMP_neq,    O,   Regsrc_X,   O,     O,       I,    O,      O,       I,     ALU_X),
      BGEZ   ->List( EX_N,     O,     O,     O,    CMP_bgez,   O,   Regsrc_X,   O,     O,       I,    O,      O,       I,     ALU_X),
      BGTZ   ->List( EX_N,     O,     O,     O,    CMP_bgtz,   O,   Regsrc_X,   O,     O,       I,    O,      O,       I,     ALU_X),
      BLEZ   ->List( EX_N,     O,     O,     O,    CMP_blez,   O,   Regsrc_X,   O,     O,       I,    O,      O,       I,     ALU_X),
      BLTZ   ->List( EX_N,     O,     O,     O,    CMP_bltz,   O,   Regsrc_X,   O,     O,       I,    O,      O,       I,     ALU_X),
      BLTZAL ->List( EX_N,     O,     I,     O,    CMP_bltz,   X,   Regsrc_X,   I,     O,       I,    O,      O,       I,     ALU_X),
      BGEZAL ->List( EX_N,     O,     I,     O,    CMP_bgez,   X,   Regsrc_X,   I,     O,       I,    O,      O,       I,     ALU_X),
      J      ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_X,   O,     O,       I,    I,      O,       O,     ALU_X),
      JAL    ->List( EX_N,     O,     I,     O,    CMP_X,      X,   Regsrc_X,   I,     O,       I,    I,      O,       X,     ALU_X),
      JR     ->List( EX_N,     I,     O,     O,    CMP_X,      O,   Regsrc_X,   O,     O,       I,    I,      O,       O,     ALU_X),
      JALR   ->List( EX_N,     I,     I,     O,    CMP_X,      X,   Regsrc_X,   I,     O,       I,    I,      O,       X,     ALU_X),
      MFHI   ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       O,     MF_hi),
      MFLO   ->List( EX_N,     O,     O,     O,    CMP_X,      I,   Regsrc_Alu, I,     O,       O,    O,      O,       O,     MF_lo),
      MTHI   ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_X,   O,     O,       O,    O,      O,       O,     MT_hi),
      MTLO   ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_X,   O,     O,       O,    O,      O,       O,     MT_lo),
      LB     ->List( EX_N,     O,     O,     O,    LS_B,       O,   Regsrc_Mem, I,     O,       O,    O,      I,       I,     ALU_Add),
      LBU    ->List( EX_N,     O,     O,     O,    LS_Bu,      O,   Regsrc_Mem, I,     O,       O,    O,      I,       I,     ALU_Add),
      LH     ->List( EX_N,     O,     O,     O,    LS_H,       O,   Regsrc_Mem, I,     O,       O,    O,      I,       I,     ALU_Add),
      LHU    ->List( EX_N,     O,     O,     O,    LS_Hu,      O,   Regsrc_Mem, I,     O,       O,    O,      I,       I,     ALU_Add),
      SB     ->List( EX_N,     O,     O,     O,    LS_B,       O,   Regsrc_Mem, O,     I,       O,    O,      I,       I,     ALU_Add),
      SH     ->List( EX_N,     O,     O,     O,    LS_H,       O,   Regsrc_Mem, O,     I,       O,    O,      I,       I,     ALU_Add),
      LW     ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_Mem, I,     O,       O,    O,      I,       I,     ALU_Add),
      SW     ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_Mem, O,     I,       O,    O,      I,       I,     ALU_Add),
      BREAK  ->List( EX_bre,   O,     O,     O,    CMP_X,      O,   Regsrc_X,   O,     O,       O,    O,      O,       O,     ALU_X),
      SYSCALL->List( EX_sys,   O,     O,     O,    CMP_X,      O,   Regsrc_X,   O,     O,       O,    O,      O,       O,     ALU_X),
      ERET   ->List( EX_N,     O,     O,     O,    Eret,       O,   Regsrc_X,   O,     O,       I,    I,      O,       O,     ALU_X),
      MFC0   ->List( EX_N,     I,     O,     O,    MF_CO,      O,   Regsrc_Alu, I,     O,       O,    O,      O,       O,     ALU_X),
      MTC0   ->List( EX_N,     I,     O,     O,    MT_CO,      O,   Regsrc_X,   O,     O,       O,    O,      O,       O,     ALU_X),
      NOP    ->List( EX_N,     O,     O,     O,    CMP_X,      O,   Regsrc_X,   O,     O,       O,    O,      O,       O,     ALU_X)
    ))

  Exc    := control(0)
  Jr     := control(1)
  AL     := control(2)
  Shamt  := control(3)
  CMPctr := control(4)
  RegDst := control(5)
  RegSrc := control(6)
  RegWr  := control(7)
  MemWr  := control(8)
  BorJ   := control(9)
  Jump   := control(10)
  ALUsrc := control(11)
  ExtOp  := control(12)
  ALUctr := control(13)

  val Imm16 = Mux((ALUctr === 4.U) || (Jump === true.B), 0.U, IF_ID_ins(15, 0))
  Imm26 := Mux(Jump === true.B, IF_ID_ins(25, 0), 0.U)
  Imm32 := Mux(ExtOp, Cat(Fill(17, Imm16(15)), Imm16(14, 0)),//有符号
    Cat("b0".U(16.W), Imm16(15, 0)))//无符号

  //----------------------------------------//
  //------------------EX--------------------//
  //----------------------------------------//

  //-----------ALU模块----------------//

  //加入数据冒险的旁路功能
  dataCheck.io.ID_EX_Rs := ID_Ex_Rs
  dataCheck.io.ID_EX_Rt := ID_EX_Rt

  dataCheck.io.Mem_WB_Rd := MEM_WB_reg_index
  dataCheck.io.Ex_Mem_Rd := EX_MEM_reg_index
  dataCheck.io.Ex_Mem_RegWr := EX_MEM_RegWr
  dataCheck.io.Mem_WB_RegWr := MEM_WB_RegWr

  val input1 = MuxCase(ID_EX_BusA,Array(
    (dataCheck.io.ForwardA === "b01".U(2.W)) -> Reg_WrData,
    (dataCheck.io.ForwardA === "b10".U(2.W)) -> EX_MEM_ALUout
  ))
  val input2 = MuxCase(ID_EX_BusB,Array(
    (dataCheck.io.ForwardB === "b01".U(2.W)) -> Reg_WrData,
    (dataCheck.io.ForwardB === "b10".U(2.W)) -> EX_MEM_ALUout
  ))

  alu.io.alu_op 	:= ID_EX_ALUctr
  alu.io.in1 	:= Mux(ID_EX_Shamt, ID_EX_Imm32,input1)//选立即数的优先级高于数据冒险
  alu.io.in2 	:= Mux(ID_EX_ALUsrc, ID_EX_Imm32,input2)//选立即数的优先级高于数据冒险

  //-----------CMP模块----------------//
  //与ALU是并行的，只是为了写起来清晰一些而分开，本质一样
  cmp.io.in1 := alu.io.in1
  cmp.io.in2 := alu.io.in2
  cmp.io.CMPctr := ID_EX_CMPctr
  CMPout := cmp.io.CMPout


  //-----------CP0协处理器模块--------------//
  //用于异常处理

  //实现MFC0,MTC0,ERET指令
  cp0.io.CP0ctr := ID_EX_CMPctr
  cp0.io.datain := input2
  cp0.io.index  := ID_EX_Rd

  //产生刷新信号
  IF_ID_Flush := cp0.io.IF_ID_Flush
  ID_EX_Flush := cp0.io.ID_EX_Flush
  EX_MEM_Flush := cp0.io.EX_MEM_Flush
  MEM_WB_Flush := cp0.io.MEM_WB_Flush

  //判断是否为延迟槽中的指令
  when(EX_MEM_BorJ){
    cp0.io.BD := 1.U
    cp0.io.EPC := ID_EX_pcPlus4 - 8.U
  }.otherwise({
    cp0.io.BD := 0.U
    cp0.io.EPC := ID_EX_pcPlus4 - 4.U
  })

    //overflow
  when(alu.io.overflow){
    cp0.io.IsExc := 1.U
    cp0.io.Status := 0x2.U
    cp0.io.ExcCode := 0x0c.U(5.W)
    cp0.io.BadVAddr := 0.U(32.W)


    //lh
  }.elsewhen(ID_EX_CMPctr === LS_H && ID_EX_RegWr && alu.io.alu_out(0)=/=0.U){
    cp0.io.IsExc := 1.U
    cp0.io.Status := 0x2.U
    cp0.io.ExcCode := 0x04.U(5.W)
    cp0.io.BadVAddr := alu.io.alu_out


    //lhu
  }.elsewhen(ID_EX_CMPctr === LS_Hu && ID_EX_RegWr && alu.io.alu_out(0)=/=0.U){
    cp0.io.IsExc := 1.U
    cp0.io.Status := 0x2.U
    cp0.io.ExcCode := 0x04.U(5.W)
    cp0.io.BadVAddr := alu.io.alu_out


    //lw
  }.elsewhen(ID_EX_CMPctr === CMP_X && ID_EX_RegSrc === Regsrc_Mem && ID_EX_RegWr && alu.io.alu_out(1,0)=/=0.U){
    cp0.io.IsExc := 1.U
    cp0.io.Status := 0x2.U
    cp0.io.ExcCode := 0x04.U(5.W)
    cp0.io.BadVAddr := alu.io.alu_out


    //sh
  }.elsewhen(ID_EX_CMPctr === LS_H && ID_EX_MemWr && alu.io.alu_out(0)=/=0.U){
    cp0.io.IsExc := 1.U
    cp0.io.Status := 0x2.U
    cp0.io.ExcCode := 0x05.U(5.W)
    cp0.io.BadVAddr := alu.io.alu_out


    //sw
  }.elsewhen(ID_EX_CMPctr === CMP_X && ID_EX_RegSrc === Regsrc_Mem && ID_EX_MemWr && alu.io.alu_out(1,0)=/=0.U){
    cp0.io.IsExc := 1.U
    cp0.io.Status := 0x2.U
    cp0.io.ExcCode := 0x05.U(5.W)
    cp0.io.BadVAddr := alu.io.alu_out


    //reserved instruction
  }.elsewhen(ID_EX_Exc === EX_res){
    cp0.io.IsExc := 1.U
    cp0.io.Status := 0x2.U
    cp0.io.ExcCode := 0x0a.U(5.W)
    cp0.io.BadVAddr := 0.U


    //break
  }.elsewhen(ID_EX_Exc === EX_bre){
    cp0.io.IsExc := 1.U
    cp0.io.Status := 0x2.U
    cp0.io.ExcCode := 0x09.U(5.W)
    cp0.io.BadVAddr := 0.U


    //syscall
  }.elsewhen(ID_EX_Exc === EX_sys){
    cp0.io.IsExc := 1.U
    cp0.io.Status := 0x2.U
    cp0.io.ExcCode := 0x08.U(5.W)
    cp0.io.BadVAddr := 0.U


    //eret_ex
  }.elsewhen(ID_EX_CMPctr === Eret ){
    cp0.io.IsExc := 0.U
    cp0.io.Status := 0x2.U
    cp0.io.ExcCode := 0x04.U(5.W)
    cp0.io.BadVAddr := 0.U


  }.otherwise({
    cp0.io.IsExc := 0.U
    cp0.io.Status := 0.U
    cp0.io.ExcCode := 0.U
    cp0.io.BadVAddr := 0.U
  })

  /*
  printf("in1=0x%x,in2=0x%x,,ALUout=0x%x,FA=0x%x,FB=0x%x,CMP=%d,npc=%d",
    alu.io.in1,alu.io.in2,alu.io.ALUout,dataCheck.io.ForwardA,dataCheck.io.ForwardB,CMPout,nPC_sel)
  */
  /*
  printf("in1=0x%x,in2=0x%x,,ALUout=0x%x,ov?=%d",
    alu.io.in1,alu.io.in2,alu.io.ALUout,alu.io.overflow)
  */


  //-----------next pc unit-----------//
  //跳转指令后的下一条指令一定执行，即延迟槽
  //如果发生跳转，则将后面的第二条指令置为气泡

  //判断会不会发生跳转
  nPC_sel := Mux(io.boot, false.B,
    Mux(ID_EX_Jump, true.B, CMPout & ID_EX_BorJ))

  io.imem_addr := pc_reg
  pc_plus4:= pc_reg + 4.U(32.W)
  //分支指令跳转地址
  val br_next= (ID_EX_Imm32<<2.U).asUInt()

  //ERET指令与J型指令的跳转
  val j_target = Wire(UInt(32.W))
  when(ID_EX_CMPctr === Eret){
    j_target := cp0.io.dataout
  }.otherwise({
    j_target := Mux(ID_EX_Jr,input1,Cat(ID_EX_pcPlus4(31, 28), ID_EX_Imm26, 0.U(2.W)))
  })

  pc_br := ID_EX_pcPlus4+br_next

  pc_next := MuxCase(pc_plus4, Array(
    (io.boot === true.B)	 -> 0.U,
    (cp0.io.ExcHapp === true.B && io.test_exc === 0.U)	 -> 0x000040b0.U(32.W),
    (cp0.io.ExcHapp === true.B && io.test_exc === 1.U )	 -> 0xfc01968.U(32.W),
    (cp0.io.ExcHapp === true.B && io.test_exc === 2.U )	 -> 0xfc015a8.U(32.W),
    (cp0.io.ExcHapp === true.B && io.test_exc === 3.U )	 -> 0xfc0a0f8.U(32.W),
    (nPC_sel === false.B)	 -> pc_plus4,
    (ID_EX_Jump === false.B && nPC_sel === true.B)		 -> pc_br,
    (ID_EX_Jump === true.B)	 -> j_target
  ))


  //-----------predict-----------//
  val BHT = Mem(2000000,UInt(2.W))
  val GBP = RegInit(init = 0.U(2.W))
  io.Branch := ID_EX_BorJ && (!ID_EX_Jump)
  io.Bhappen := nPC_sel
  //BHT state
  val now_state = BHT(ID_EX_pcPlus4)
  when(now_state === 0.U){
    when(nPC_sel){
      BHT(ID_EX_pcPlus4) := 1.U
    }.otherwise({
      BHT(ID_EX_pcPlus4) := 0.U
    })
  }.elsewhen(now_state === 1.U){
    when(nPC_sel){
      BHT(ID_EX_pcPlus4) := 2.U
    }.otherwise({
      BHT(ID_EX_pcPlus4) := 0.U
    })


  }.elsewhen(now_state === 2.U){
    when(nPC_sel){
      BHT(ID_EX_pcPlus4) := 3.U
    }.otherwise({
      BHT(ID_EX_pcPlus4) := 1.U
    })


  }.elsewhen(now_state === 3.U){
    when(nPC_sel){
      BHT(ID_EX_pcPlus4) := 3.U
    }.otherwise({
      BHT(ID_EX_pcPlus4) := 2.U
    })
  }

  //GBP state
  when(GBP === 0.U){
    when(nPC_sel){
      GBP := 1.U
    }.otherwise({
      GBP := 0.U
    })


  }.elsewhen(GBP === 1.U){
    when(nPC_sel){
      GBP := 2.U
    }.otherwise({
      GBP := 0.U
    })


  }.elsewhen(GBP === 2.U) {
    when(nPC_sel) {
      GBP := 3.U
    }.otherwise({
      GBP := 1.U
    })


  }.otherwise({
    when(nPC_sel) {
      GBP := 3.U
    }.otherwise({
      GBP := 2.U
    })
  })


  when(now_state(1)===1.U){
    io.Bpredict := true.B

  }.elsewhen(now_state===1.U){
    io.Bpredict := false.B

  }.otherwise({
    io.Bpredict := GBP(1)
  })

  //----------------------------------------//
  //------------------MEM-------------------//
  //----------------------------------------//

  //Address for write and read
  val Addr0 = EX_MEM_ALUout
  val Addr1 = EX_MEM_ALUout+1.U
  val Addr2 = EX_MEM_ALUout+2.U
  val Addr3 = EX_MEM_ALUout+3.U

  //解决了R型后跟Store的数据冒险
  when(EX_MEM_Rt === Reg_index && MEM_WB_RegWr === true.B){
    Memin := Reg_WrData
  }.otherwise({
    Memin := EX_MEM_dataIn
  })
  //写存储器
  when(EX_MEM_MemWr) {
    when(EX_MEM_LSctr === LS_B){
      dmm(Addr0) := Memin(7,0)
    }.elsewhen(EX_MEM_LSctr === LS_H){
      dmm(Addr0) := Memin(7,0)
      dmm(Addr1) := Memin(15,8)
    }.otherwise({
      dmm(Addr0) := Memin(7,0)
      dmm(Addr1) := Memin(15,8)
      dmm(Addr2) := Memin(23,16)
      dmm(Addr3) := Memin(31,24)
    })
  }

  //读存储器
  val Data0 = dmm(Addr0)
  val Data1 = dmm(Addr1)
  val Data2 = dmm(Addr2)
  val Data3 = dmm(Addr3)

  //读存储器
  when(EX_MEM_LSctr === LS_B){
    Memout := Cat(Fill(25,Data0(7)),Data0(6,0))

  }.elsewhen(EX_MEM_LSctr === LS_Bu){
    Memout := Cat(Fill(24,0.U(1.W)),Data0)

  }.elsewhen(EX_MEM_LSctr === LS_H){
    Memout := Cat(Fill(17,Data1(7)),Data1(6,0),Data0)

  }.elsewhen(EX_MEM_LSctr === LS_Hu){
    Memout := Cat(Fill(16,0.U(1.W)),Data1,Data0)

  }.otherwise({
    Memout := Cat(Data3,Data2,Data1,Data0)
  })


  /*
  printf("Address=0x%x,DataRe=0x%x,DataWr=0x%x,MemWr?=%d",
    Addr0,
    Cat(Data0,Data1,Data2,Data3),
    EX_MEM_dataIn,
    EX_MEM_MemWr
    )
  */

  //----------------------------------------//
  //------------------WB-------------------//
  //----------------------------------------//

  when(MEM_WB_AL){//AL为高时 需要写回pc+8值到31号寄存器
    Reg_WrData := MEM_WB_pcAL
  }.otherwise({
    Reg_WrData := MuxCase(MEM_WB_ALUout,Array(
      (MEM_WB_RegSrc === Regsrc_CMP) -> MEM_WB_CMPout,
      (MEM_WB_RegSrc === Regsrc_Mem) -> MEM_WB_MemOut,
      (MEM_WB_RegSrc === Regsrc_Alu) -> MEM_WB_ALUout
    ))
  })

  Reg_index := MEM_WB_reg_index

  // Write Register
  when (MEM_WB_RegWr === true.B) {
    RegFile(Reg_index) := Reg_WrData
  }
}