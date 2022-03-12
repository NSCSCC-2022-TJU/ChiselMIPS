module ALU(
  input         clock,
  input         reset,
  input  [31:0] io_in1,
  input  [31:0] io_in2,
  input  [5:0]  io_alu_op,
  output [31:0] io_alu_out,
  output        io_overflow
);
`ifdef RANDOMIZE_REG_INIT
  reg [31:0] _RAND_0;
  reg [31:0] _RAND_1;
`endif // RANDOMIZE_REG_INIT
  reg [31:0] hi; // @[ALU.scala 22:21]
  reg [31:0] lo; // @[ALU.scala 23:21]
  wire [31:0] _io_alu_out_T_1 = io_in1 + io_in2; // @[ALU.scala 31:30]
  wire  _io_overflow_T_4 = $signed(io_alu_out) > 32'sh0 & $signed(io_in1) < 32'sh0; // @[ALU.scala 37:49]
  wire  _io_overflow_T_6 = $signed(io_in2) < 32'sh0; // @[ALU.scala 37:90]
  wire  _io_overflow_T_12 = $signed(io_alu_out) < 32'sh0 & $signed(io_in1) > 32'sh0; // @[ALU.scala 38:63]
  wire  _io_overflow_T_14 = $signed(io_in2) > 32'sh0; // @[ALU.scala 38:104]
  wire  _io_overflow_T_15 = $signed(io_alu_out) < 32'sh0 & $signed(io_in1) > 32'sh0 & $signed(io_in2) > 32'sh0; // @[ALU.scala 38:86]
  wire [31:0] _io_alu_out_T_5 = io_in1 - io_in2; // @[ALU.scala 42:30]
  wire  _io_overflow_T_32 = _io_overflow_T_4 & _io_overflow_T_14; // @[ALU.scala 49:86]
  wire [31:0] _io_alu_out_T_8 = io_in1 & io_in2; // @[ALU.scala 53:30]
  wire [31:0] _io_alu_out_T_9 = io_in1 | io_in2; // @[ALU.scala 58:32]
  wire [31:0] _io_alu_out_T_10 = ~_io_alu_out_T_9; // @[ALU.scala 58:23]
  wire [31:0] _io_alu_out_T_12 = io_in1 ^ io_in2; // @[ALU.scala 68:30]
  wire [47:0] _GEN_92 = {io_in2, 16'h0}; // @[ALU.scala 73:30]
  wire [62:0] _io_alu_out_T_13 = {{15'd0}, _GEN_92}; // @[ALU.scala 73:30]
  wire [62:0] _GEN_0 = {{31'd0}, io_in2}; // @[ALU.scala 78:30]
  wire [62:0] _io_alu_out_T_15 = _GEN_0 << io_in1[10:6]; // @[ALU.scala 78:30]
  wire [65566:0] _GEN_3 = {{65535'd0}, io_in2}; // @[ALU.scala 83:30]
  wire [65566:0] _io_alu_out_T_17 = _GEN_3 << io_in1[15:0]; // @[ALU.scala 83:30]
  wire [31:0] _io_alu_out_T_19 = io_in2 >> io_in1[10:6]; // @[ALU.scala 88:30]
  wire [31:0] _io_alu_out_T_21 = io_in2 >> io_in1[15:0]; // @[ALU.scala 93:30]
  wire [31:0] _io_alu_out_T_25 = $signed(io_in2) >>> io_in1[10:6]; // @[ALU.scala 98:63]
  wire [31:0] _io_alu_out_T_29 = $signed(io_in2) >>> io_in1[15:0]; // @[ALU.scala 103:63]
  wire [63:0] mul = $signed(io_in1) * $signed(io_in2); // @[ALU.scala 131:61]
  wire [63:0] mul_1 = io_in1 * io_in2; // @[ALU.scala 139:26]
  wire [31:0] _hi_T_5 = $signed(io_in1) % $signed(io_in2); // @[ALU.scala 147:57]
  wire [32:0] _lo_T_5 = $signed(io_in1) / $signed(io_in2); // @[ALU.scala 148:59]
  wire [31:0] _GEN_4 = io_in1 % io_in2; // @[ALU.scala 154:22]
  wire [31:0] _hi_T_6 = _GEN_4[31:0]; // @[ALU.scala 154:22]
  wire [31:0] _lo_T_6 = io_in1 / io_in2; // @[ALU.scala 155:23]
  wire [31:0] _GEN_1 = io_alu_op == 6'h2b ? _hi_T_6 : hi; // @[ALU.scala 152:40 154:12 22:21]
  wire [31:0] _GEN_2 = io_alu_op == 6'h2b ? _lo_T_6 : lo; // @[ALU.scala 152:40 155:12 23:21]
  wire [31:0] _GEN_5 = io_alu_op == 6'h2a ? _hi_T_5 : _GEN_1; // @[ALU.scala 145:39 147:12]
  wire [32:0] _GEN_6 = io_alu_op == 6'h2a ? _lo_T_5 : {{1'd0}, _GEN_2}; // @[ALU.scala 145:39 148:12]
  wire [31:0] _GEN_9 = io_alu_op == 6'h29 ? mul_1[63:32] : _GEN_5; // @[ALU.scala 137:41 140:12]
  wire [32:0] _GEN_10 = io_alu_op == 6'h29 ? {{1'd0}, mul_1[31:0]} : _GEN_6; // @[ALU.scala 137:41 141:12]
  wire [31:0] _GEN_13 = io_alu_op == 6'h28 ? mul[63:32] : _GEN_9; // @[ALU.scala 129:40 132:12]
  wire [32:0] _GEN_14 = io_alu_op == 6'h28 ? {{1'd0}, mul[31:0]} : _GEN_10; // @[ALU.scala 129:40 133:12]
  wire [31:0] _GEN_16 = io_alu_op == 6'h13 ? lo : 32'h0; // @[ALU.scala 124:37 125:20]
  wire [31:0] _GEN_18 = io_alu_op == 6'h13 ? hi : _GEN_13; // @[ALU.scala 124:37 22:21]
  wire [32:0] _GEN_19 = io_alu_op == 6'h13 ? {{1'd0}, lo} : _GEN_14; // @[ALU.scala 124:37 23:21]
  wire [31:0] _GEN_20 = io_alu_op == 6'h12 ? hi : _GEN_16; // @[ALU.scala 119:37 120:20]
  wire [31:0] _GEN_22 = io_alu_op == 6'h12 ? hi : _GEN_18; // @[ALU.scala 119:37 22:21]
  wire [32:0] _GEN_23 = io_alu_op == 6'h12 ? {{1'd0}, lo} : _GEN_19; // @[ALU.scala 119:37 23:21]
  wire [32:0] _GEN_24 = io_alu_op == 6'h11 ? {{1'd0}, io_in1} : _GEN_23; // @[ALU.scala 113:37 114:12]
  wire [31:0] _GEN_25 = io_alu_op == 6'h11 ? 32'h0 : _GEN_20; // @[ALU.scala 113:37 115:20]
  wire [31:0] _GEN_27 = io_alu_op == 6'h11 ? hi : _GEN_22; // @[ALU.scala 113:37 22:21]
  wire [31:0] _GEN_28 = io_alu_op == 6'h10 ? io_in1 : _GEN_27; // @[ALU.scala 107:37 108:12]
  wire [31:0] _GEN_29 = io_alu_op == 6'h10 ? 32'h0 : _GEN_25; // @[ALU.scala 107:37 109:20]
  wire [32:0] _GEN_31 = io_alu_op == 6'h10 ? {{1'd0}, lo} : _GEN_24; // @[ALU.scala 107:37 23:21]
  wire [31:0] _GEN_32 = io_alu_op == 6'h7 ? _io_alu_out_T_29 : _GEN_29; // @[ALU.scala 102:40 103:20]
  wire [31:0] _GEN_34 = io_alu_op == 6'h7 ? hi : _GEN_28; // @[ALU.scala 102:40 22:21]
  wire [32:0] _GEN_35 = io_alu_op == 6'h7 ? {{1'd0}, lo} : _GEN_31; // @[ALU.scala 102:40 23:21]
  wire [31:0] _GEN_36 = io_alu_op == 6'h3 ? _io_alu_out_T_25 : _GEN_32; // @[ALU.scala 97:39 98:20]
  wire [31:0] _GEN_38 = io_alu_op == 6'h3 ? hi : _GEN_34; // @[ALU.scala 22:21 97:39]
  wire [32:0] _GEN_39 = io_alu_op == 6'h3 ? {{1'd0}, lo} : _GEN_35; // @[ALU.scala 23:21 97:39]
  wire [31:0] _GEN_40 = io_alu_op == 6'h6 ? _io_alu_out_T_21 : _GEN_36; // @[ALU.scala 92:40 93:20]
  wire [31:0] _GEN_42 = io_alu_op == 6'h6 ? hi : _GEN_38; // @[ALU.scala 22:21 92:40]
  wire [32:0] _GEN_43 = io_alu_op == 6'h6 ? {{1'd0}, lo} : _GEN_39; // @[ALU.scala 23:21 92:40]
  wire [31:0] _GEN_44 = io_alu_op == 6'h2 ? _io_alu_out_T_19 : _GEN_40; // @[ALU.scala 87:39 88:20]
  wire [31:0] _GEN_46 = io_alu_op == 6'h2 ? hi : _GEN_42; // @[ALU.scala 22:21 87:39]
  wire [32:0] _GEN_47 = io_alu_op == 6'h2 ? {{1'd0}, lo} : _GEN_43; // @[ALU.scala 23:21 87:39]
  wire [65566:0] _GEN_48 = io_alu_op == 6'h4 ? _io_alu_out_T_17 : {{65535'd0}, _GEN_44}; // @[ALU.scala 82:40 83:20]
  wire [31:0] _GEN_50 = io_alu_op == 6'h4 ? hi : _GEN_46; // @[ALU.scala 22:21 82:40]
  wire [32:0] _GEN_51 = io_alu_op == 6'h4 ? {{1'd0}, lo} : _GEN_47; // @[ALU.scala 23:21 82:40]
  wire [65566:0] _GEN_52 = io_alu_op == 6'h0 ? {{65504'd0}, _io_alu_out_T_15} : _GEN_48; // @[ALU.scala 77:39 78:20]
  wire [31:0] _GEN_54 = io_alu_op == 6'h0 ? hi : _GEN_50; // @[ALU.scala 22:21 77:39]
  wire [32:0] _GEN_55 = io_alu_op == 6'h0 ? {{1'd0}, lo} : _GEN_51; // @[ALU.scala 23:21 77:39]
  wire [65566:0] _GEN_56 = io_alu_op == 6'h3f ? {{65504'd0}, _io_alu_out_T_13} : _GEN_52; // @[ALU.scala 72:39 73:20]
  wire [31:0] _GEN_58 = io_alu_op == 6'h3f ? hi : _GEN_54; // @[ALU.scala 22:21 72:39]
  wire [32:0] _GEN_59 = io_alu_op == 6'h3f ? {{1'd0}, lo} : _GEN_55; // @[ALU.scala 23:21 72:39]
  wire [65566:0] _GEN_60 = io_alu_op == 6'h26 ? {{65535'd0}, _io_alu_out_T_12} : _GEN_56; // @[ALU.scala 67:39 68:20]
  wire [31:0] _GEN_62 = io_alu_op == 6'h26 ? hi : _GEN_58; // @[ALU.scala 22:21 67:39]
  wire [32:0] _GEN_63 = io_alu_op == 6'h26 ? {{1'd0}, lo} : _GEN_59; // @[ALU.scala 23:21 67:39]
  wire [65566:0] _GEN_64 = io_alu_op == 6'h25 ? {{65535'd0}, _io_alu_out_T_9} : _GEN_60; // @[ALU.scala 62:38 63:20]
  wire [31:0] _GEN_66 = io_alu_op == 6'h25 ? hi : _GEN_62; // @[ALU.scala 22:21 62:38]
  wire [32:0] _GEN_67 = io_alu_op == 6'h25 ? {{1'd0}, lo} : _GEN_63; // @[ALU.scala 23:21 62:38]
  wire [65566:0] _GEN_68 = io_alu_op == 6'h27 ? {{65535'd0}, _io_alu_out_T_10} : _GEN_64; // @[ALU.scala 57:39 58:20]
  wire [31:0] _GEN_70 = io_alu_op == 6'h27 ? hi : _GEN_66; // @[ALU.scala 22:21 57:39]
  wire [32:0] _GEN_71 = io_alu_op == 6'h27 ? {{1'd0}, lo} : _GEN_67; // @[ALU.scala 23:21 57:39]
  wire [65566:0] _GEN_72 = io_alu_op == 6'h24 ? {{65535'd0}, _io_alu_out_T_8} : _GEN_68; // @[ALU.scala 52:39 53:20]
  wire [31:0] _GEN_74 = io_alu_op == 6'h24 ? hi : _GEN_70; // @[ALU.scala 22:21 52:39]
  wire [32:0] _GEN_75 = io_alu_op == 6'h24 ? {{1'd0}, lo} : _GEN_71; // @[ALU.scala 23:21 52:39]
  wire [65566:0] _GEN_76 = io_alu_op == 6'h23 ? {{65535'd0}, _io_alu_out_T_5} : _GEN_72; // @[ALU.scala 46:41 47:20]
  wire  _GEN_77 = io_alu_op == 6'h23 & (_io_overflow_T_12 & _io_overflow_T_6 | _io_overflow_T_32); // @[ALU.scala 46:41 48:21]
  wire [31:0] _GEN_78 = io_alu_op == 6'h23 ? hi : _GEN_74; // @[ALU.scala 22:21 46:41]
  wire [32:0] _GEN_79 = io_alu_op == 6'h23 ? {{1'd0}, lo} : _GEN_75; // @[ALU.scala 23:21 46:41]
  wire [65566:0] _GEN_80 = io_alu_op == 6'h22 ? {{65535'd0}, _io_alu_out_T_5} : _GEN_76; // @[ALU.scala 41:39 42:20]
  wire  _GEN_81 = io_alu_op == 6'h22 ? 1'h0 : _GEN_77; // @[ALU.scala 26:21 41:39]
  wire [32:0] _GEN_83 = io_alu_op == 6'h22 ? {{1'd0}, lo} : _GEN_79; // @[ALU.scala 23:21 41:39]
  wire [65566:0] _GEN_84 = io_alu_op == 6'h21 ? {{65535'd0}, _io_alu_out_T_1} : _GEN_80; // @[ALU.scala 35:42 36:20]
  wire  _GEN_85 = io_alu_op == 6'h21 ? $signed(io_alu_out) > 32'sh0 & $signed(io_in1) < 32'sh0 & $signed(io_in2) < 32'sh0
     | _io_overflow_T_15 : _GEN_81; // @[ALU.scala 35:42 37:21]
  wire [32:0] _GEN_87 = io_alu_op == 6'h21 ? {{1'd0}, lo} : _GEN_83; // @[ALU.scala 23:21 35:42]
  wire [65566:0] _GEN_88 = io_alu_op == 6'h20 ? {{65535'd0}, _io_alu_out_T_1} : _GEN_84; // @[ALU.scala 30:34 31:20]
  wire [32:0] _GEN_91 = io_alu_op == 6'h20 ? {{1'd0}, lo} : _GEN_87; // @[ALU.scala 23:21 30:34]
  wire [32:0] _GEN_93 = reset ? 33'h0 : _GEN_91; // @[ALU.scala 23:{21,21}]
  assign io_alu_out = _GEN_88[31:0];
  assign io_overflow = io_alu_op == 6'h20 ? 1'h0 : _GEN_85; // @[ALU.scala 26:21 30:34]
  always @(posedge clock) begin
    if (reset) begin // @[ALU.scala 22:21]
      hi <= 32'h0; // @[ALU.scala 22:21]
    end else if (!(io_alu_op == 6'h20)) begin // @[ALU.scala 30:34]
      if (!(io_alu_op == 6'h21)) begin // @[ALU.scala 35:42]
        if (!(io_alu_op == 6'h22)) begin // @[ALU.scala 41:39]
          hi <= _GEN_78;
        end
      end
    end
    lo <= _GEN_93[31:0]; // @[ALU.scala 23:{21,21}]
  end
// Register and memory initialization
`ifdef RANDOMIZE_GARBAGE_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_INVALID_ASSIGN
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_REG_INIT
`define RANDOMIZE
`endif
`ifdef RANDOMIZE_MEM_INIT
`define RANDOMIZE
`endif
`ifndef RANDOM
`define RANDOM $random
`endif
`ifdef RANDOMIZE_MEM_INIT
  integer initvar;
`endif
`ifndef SYNTHESIS
`ifdef FIRRTL_BEFORE_INITIAL
`FIRRTL_BEFORE_INITIAL
`endif
initial begin
  `ifdef RANDOMIZE
    `ifdef INIT_RANDOM
      `INIT_RANDOM
    `endif
    `ifndef VERILATOR
      `ifdef RANDOMIZE_DELAY
        #`RANDOMIZE_DELAY begin end
      `else
        #0.002 begin end
      `endif
    `endif
`ifdef RANDOMIZE_REG_INIT
  _RAND_0 = {1{`RANDOM}};
  hi = _RAND_0[31:0];
  _RAND_1 = {1{`RANDOM}};
  lo = _RAND_1[31:0];
`endif // RANDOMIZE_REG_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
