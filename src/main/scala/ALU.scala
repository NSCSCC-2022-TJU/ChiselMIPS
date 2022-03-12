package Pipeline

import chisel3._
import chisel3.util._
import Define._

class ALU_IO extends Bundle {
    //input
    val in1 = Input(UInt(32.W))
    val in2 = Input(UInt(32.W))
    val alu_op = Input(UInt(6.W))
    //output
    val alu_out = Output(UInt(32.W))
    val overflow = Output(Bool())

}

class ALU extends Module {
    //io
    val io = IO(new ALU_IO)

    val hi = RegInit(0.U(32.W))
    val lo = RegInit(0.U(32.W))

    def Unuse_overflow() ={
        io.overflow := false.B
    }

        //ADD
    when (io.alu_op === ALU_Add) {
        io.alu_out := io.in1 + io.in2
        Unuse_overflow()

        //ADDEX
    }.elsewhen(io.alu_op === ALU_Addex ) {
        io.alu_out := io.in1 + io.in2
        io.overflow := (io.alu_out.asSInt()>0.S && io.in1.asSInt()<0.S && io.in2.asSInt()<0.S) || 
                                     (io.alu_out.asSInt()<0.S && io.in1.asSInt()>0.S && io.in2.asSInt()>0.S)

        //SUB
    }.elsewhen(io.alu_op === ALU_Sub) {
        io.alu_out := io.in1 - io.in2
        Unuse_overflow()

        //SUBEX
    }.elsewhen(io.alu_op === ALU_Subex) {
        io.alu_out := io.in1 - io.in2
        io.overflow := (io.alu_out.asSInt()<0.S && io.in1.asSInt()>0.S && io.in2.asSInt()<0.S) || 
                                     (io.alu_out.asSInt()>0.S && io.in1.asSInt()<0.S && io.in2.asSInt()>0.S)

        //AND
    }.elsewhen(io.alu_op === ALU_And) {
        io.alu_out := io.in1 & io.in2
        Unuse_overflow()

        //NOR
    }.elsewhen(io.alu_op === ALU_Nor) {
        io.alu_out := ~(io.in1 | io.in2)
        Unuse_overflow()

        //OR
    }.elsewhen(io.alu_op === ALU_Or) {
        io.alu_out := io.in1 | io.in2
        Unuse_overflow()

        //XOR
    }.elsewhen(io.alu_op === ALU_Xor) {
        io.alu_out := io.in1 ^ io.in2
        Unuse_overflow()

        //LUI
    }.elsewhen(io.alu_op === ALU_Lui) {
        io.alu_out := io.in2 << 16.U
        Unuse_overflow()

        //SLL
    }.elsewhen(io.alu_op === ALU_Sll) {
        io.alu_out := io.in2 << io.in1(10,6)
        Unuse_overflow()

        //SLLV
    }.elsewhen(io.alu_op === ALU_Sllv) {
        io.alu_out := io.in2 << io.in1(15,0)
        Unuse_overflow()

        //SRL
    }.elsewhen(io.alu_op === ALU_Srl) {
        io.alu_out := io.in2 >> io.in1(10,6)
        Unuse_overflow()

        //SRLV
    }.elsewhen(io.alu_op === ALU_Srlv) {
        io.alu_out := io.in2 >> io.in1(15,0)
        Unuse_overflow()

        //SRA
    }.elsewhen(io.alu_op === ALU_Sra) {
        io.alu_out := (io.in2.asSInt() >> io.in1(10,6)).asUInt()
        Unuse_overflow()

        //SRAV
    }.elsewhen(io.alu_op === ALU_Srav) {
        io.alu_out := (io.in2.asSInt() >> io.in1(15,0)).asUInt()
        Unuse_overflow()

        //MTHI
    }.elsewhen(io.alu_op === MT_hi) {
        hi := io.in1
        io.alu_out := 0.U(32.W)
        Unuse_overflow()

        //MTLO
    }.elsewhen(io.alu_op === MT_lo) {
        lo := io.in1
        io.alu_out := 0.U(32.W)
        Unuse_overflow()

        //MFHI
    }.elsewhen(io.alu_op === MF_hi) {
        io.alu_out := hi
        Unuse_overflow()

        //MFLO
    }.elsewhen(io.alu_op === MF_lo) {
        io.alu_out := lo
        Unuse_overflow()

        //MULT
    }.elsewhen(io.alu_op === ALU_Mult) {
        io.alu_out := 0.U(32.W)
        val mul = (io.in1.asSInt() * io.in2.asSInt()).asUInt()
        hi := mul(63,32)
        lo := mul(31,0)
        Unuse_overflow()

        //MULTU
    }.elsewhen(io.alu_op === ALU_Multu) {
        io.alu_out := 0.U(32.W)
        val mul = io.in1 * io.in2
        hi := mul(63,32)
        lo := mul(31,0)
        Unuse_overflow()

        //DIV
    }.elsewhen(io.alu_op === ALU_Div) {
        io.alu_out := 0.U(32.W)
        hi := (io.in1.asSInt() % io.in2.asSInt()).asUInt()
        lo := (io.in1.asSInt()  /  io.in2.asSInt()).asUInt()
        Unuse_overflow()

        //DIVU
    }.elsewhen(io.alu_op === ALU_Divu) {
        io.alu_out := 0.U(32.W)
        hi := io.in1 % io.in2
        lo := io.in1  /  io.in2
        Unuse_overflow()

        //OTHER
    }.otherwise({
        io.alu_out := 0.U(32.W)
        Unuse_overflow()
    })
}