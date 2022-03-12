module DataCheck(
  input        clock,
  input        reset,
  input  [4:0] io_ID_EX_Rs,
  input  [4:0] io_ID_EX_Rt,
  input  [4:0] io_Ex_Mem_Rd,
  input  [4:0] io_Mem_WB_Rd,
  input        io_Ex_Mem_RegWr,
  input        io_Mem_WB_RegWr,
  output [1:0] io_ForwardA,
  output [1:0] io_ForwardB
);
  wire  _T_1 = io_Ex_Mem_RegWr & io_Ex_Mem_Rd != 5'h0; // @[DATACHECK.scala 18:38]
  wire  _T_5 = io_Mem_WB_RegWr & io_Mem_WB_Rd != 5'h0; // @[DATACHECK.scala 20:44]
  wire [1:0] _GEN_0 = io_Mem_WB_RegWr & io_Mem_WB_Rd != 5'h0 & io_Mem_WB_Rd == io_ID_EX_Rs ? 2'h1 : 2'h0; // @[DATACHECK.scala 20:100 21:37 23:37]
  wire [1:0] _GEN_2 = _T_5 & io_Mem_WB_Rd == io_ID_EX_Rt ? 2'h1 : 2'h0; // @[DATACHECK.scala 28:100 29:37 31:37]
  assign io_ForwardA = io_Ex_Mem_RegWr & io_Ex_Mem_Rd != 5'h0 & io_Ex_Mem_Rd == io_ID_EX_Rs ? 2'h2 : _GEN_0; // @[DATACHECK.scala 18:94 19:37]
  assign io_ForwardB = _T_1 & io_Ex_Mem_Rd == io_ID_EX_Rt ? 2'h2 : _GEN_2; // @[DATACHECK.scala 26:94 27:37]
endmodule
