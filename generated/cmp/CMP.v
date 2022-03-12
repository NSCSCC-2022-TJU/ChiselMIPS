module CMP(
  input         clock,
  input         reset,
  input  [31:0] io_in1,
  input  [31:0] io_in2,
  input  [3:0]  io_CMPctr,
  output        io_CMPout
);
  wire  _GEN_0 = io_CMPctr == 4'ha & $signed(io_in1) < 32'sh0; // @[CMP.scala 43:35 44:15 48:27]
  wire  _GEN_1 = io_CMPctr == 4'h6 ? $signed(io_in1) <= 32'sh0 : _GEN_0; // @[CMP.scala 39:35 40:15]
  wire  _GEN_2 = io_CMPctr == 4'h3 ? $signed(io_in1) > 32'sh0 : _GEN_1; // @[CMP.scala 35:35 36:15]
  wire  _GEN_3 = io_CMPctr == 4'h5 ? $signed(io_in1) >= 32'sh0 : _GEN_2; // @[CMP.scala 31:35 32:15]
  wire  _GEN_4 = io_CMPctr == 4'h2 ? io_in1 < io_in2 : _GEN_3; // @[CMP.scala 27:41 28:27]
  wire  _GEN_5 = io_CMPctr == 4'h1 ? $signed(io_in1) < $signed(io_in2) : _GEN_4; // @[CMP.scala 23:40 24:27]
  wire  _GEN_6 = io_CMPctr == 4'h7 ? io_in1 != io_in2 : _GEN_5; // @[CMP.scala 19:40 20:27]
  assign io_CMPout = io_CMPctr == 4'h0 ? io_in1 == io_in2 : _GEN_6; // @[CMP.scala 15:30 16:27]
endmodule
