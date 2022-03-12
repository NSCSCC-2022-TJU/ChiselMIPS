module CP0(
  input         clock,
  input         reset,
  input  [4:0]  io_index,
  input  [31:0] io_datain,
  input  [3:0]  io_CP0ctr,
  output [31:0] io_dataout,
  input         io_BD,
  input  [31:0] io_Status,
  input  [4:0]  io_ExcCode,
  input  [31:0] io_BadVAddr,
  input  [31:0] io_EPC,
  input         io_IsExc,
  output        io_ExcHapp,
  output        io_IF_ID_Flush,
  output        io_ID_EX_Flush,
  output        io_EX_MEM_Flush,
  output        io_MEM_WB_Flush
);
`ifdef RANDOMIZE_MEM_INIT
  reg [31:0] _RAND_0;
`endif // RANDOMIZE_MEM_INIT
  reg [31:0] RegFile [0:31]; // @[CP0.scala 28:26]
  wire  RegFile_io_dataout_MPORT_en; // @[CP0.scala 28:26]
  wire [4:0] RegFile_io_dataout_MPORT_addr; // @[CP0.scala 28:26]
  wire [31:0] RegFile_io_dataout_MPORT_data; // @[CP0.scala 28:26]
  wire  RegFile_io_dataout_MPORT_1_en; // @[CP0.scala 28:26]
  wire [4:0] RegFile_io_dataout_MPORT_1_addr; // @[CP0.scala 28:26]
  wire [31:0] RegFile_io_dataout_MPORT_1_data; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_4_en; // @[CP0.scala 28:26]
  wire [4:0] RegFile_MPORT_4_addr; // @[CP0.scala 28:26]
  wire [31:0] RegFile_MPORT_4_data; // @[CP0.scala 28:26]
  wire [31:0] RegFile_MPORT_data; // @[CP0.scala 28:26]
  wire [4:0] RegFile_MPORT_addr; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_mask; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_en; // @[CP0.scala 28:26]
  wire [31:0] RegFile_MPORT_1_data; // @[CP0.scala 28:26]
  wire [4:0] RegFile_MPORT_1_addr; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_1_mask; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_1_en; // @[CP0.scala 28:26]
  wire [31:0] RegFile_MPORT_2_data; // @[CP0.scala 28:26]
  wire [4:0] RegFile_MPORT_2_addr; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_2_mask; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_2_en; // @[CP0.scala 28:26]
  wire [31:0] RegFile_MPORT_3_data; // @[CP0.scala 28:26]
  wire [4:0] RegFile_MPORT_3_addr; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_3_mask; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_3_en; // @[CP0.scala 28:26]
  wire [31:0] RegFile_MPORT_5_data; // @[CP0.scala 28:26]
  wire [4:0] RegFile_MPORT_5_addr; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_5_mask; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_5_en; // @[CP0.scala 28:26]
  wire [31:0] RegFile_MPORT_6_data; // @[CP0.scala 28:26]
  wire [4:0] RegFile_MPORT_6_addr; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_6_mask; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_6_en; // @[CP0.scala 28:26]
  wire [31:0] RegFile_MPORT_7_data; // @[CP0.scala 28:26]
  wire [4:0] RegFile_MPORT_7_addr; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_7_mask; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_7_en; // @[CP0.scala 28:26]
  wire [31:0] RegFile_MPORT_8_data; // @[CP0.scala 28:26]
  wire [4:0] RegFile_MPORT_8_addr; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_8_mask; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_8_en; // @[CP0.scala 28:26]
  wire [31:0] RegFile_MPORT_9_data; // @[CP0.scala 28:26]
  wire [4:0] RegFile_MPORT_9_addr; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_9_mask; // @[CP0.scala 28:26]
  wire  RegFile_MPORT_9_en; // @[CP0.scala 28:26]
  wire  _T = io_CP0ctr == 4'h9; // @[CP0.scala 38:24]
  wire  _T_1 = io_CP0ctr == 4'h8; // @[CP0.scala 44:30]
  wire  _T_2 = io_CP0ctr == 4'hf; // @[CP0.scala 50:30]
  wire  _T_3 = io_dataout > 32'hffffff; // @[CP0.scala 53:33]
  wire [6:0] cause_lo = {io_ExcCode,2'h0}; // @[Cat.scala 31:58]
  wire [24:0] cause_hi = {io_BD,24'h0}; // @[Cat.scala 31:58]
  wire [31:0] _GEN_28 = io_CP0ctr == 4'hf ? RegFile_io_dataout_MPORT_1_data : 32'h0; // @[CP0.scala 50:39 51:28]
  wire  _GEN_31 = io_CP0ctr == 4'hf & _T_3; // @[CP0.scala 28:26 50:39]
  wire  _GEN_40 = io_CP0ctr == 4'hf ? _T_3 : io_IsExc; // @[CP0.scala 50:39]
  wire  _GEN_45 = io_CP0ctr == 4'hf ? 1'h0 : io_IsExc; // @[CP0.scala 28:26 50:39]
  wire [31:0] _GEN_57 = io_CP0ctr == 4'h8 ? RegFile_io_dataout_MPORT_data : _GEN_28; // @[CP0.scala 44:40 45:28]
  wire  _GEN_58 = io_CP0ctr == 4'h8 ? 1'h0 : _GEN_40; // @[CP0.scala 31:32 44:40]
  wire  _GEN_63 = io_CP0ctr == 4'h8 ? 1'h0 : _T_2; // @[CP0.scala 28:26 44:40]
  wire  _GEN_66 = io_CP0ctr == 4'h8 ? 1'h0 : _GEN_31; // @[CP0.scala 28:26 44:40]
  wire  _GEN_77 = io_CP0ctr == 4'h8 ? 1'h0 : _GEN_45; // @[CP0.scala 28:26 44:40]
  assign RegFile_io_dataout_MPORT_en = _T ? 1'h0 : _T_1;
  assign RegFile_io_dataout_MPORT_addr = io_index;
  assign RegFile_io_dataout_MPORT_data = RegFile[RegFile_io_dataout_MPORT_addr]; // @[CP0.scala 28:26]
  assign RegFile_io_dataout_MPORT_1_en = _T ? 1'h0 : _GEN_63;
  assign RegFile_io_dataout_MPORT_1_addr = 5'he;
  assign RegFile_io_dataout_MPORT_1_data = RegFile[RegFile_io_dataout_MPORT_1_addr]; // @[CP0.scala 28:26]
  assign RegFile_MPORT_4_en = _T ? 1'h0 : _GEN_66;
  assign RegFile_MPORT_4_addr = 5'he;
  assign RegFile_MPORT_4_data = RegFile[RegFile_MPORT_4_addr]; // @[CP0.scala 28:26]
  assign RegFile_MPORT_data = io_datain;
  assign RegFile_MPORT_addr = io_index;
  assign RegFile_MPORT_mask = 1'h1;
  assign RegFile_MPORT_en = io_CP0ctr == 4'h9;
  assign RegFile_MPORT_1_data = {cause_hi,cause_lo};
  assign RegFile_MPORT_1_addr = 5'hd;
  assign RegFile_MPORT_1_mask = 1'h1;
  assign RegFile_MPORT_1_en = _T ? 1'h0 : _GEN_66;
  assign RegFile_MPORT_2_data = io_EPC;
  assign RegFile_MPORT_2_addr = 5'he;
  assign RegFile_MPORT_2_mask = 1'h1;
  assign RegFile_MPORT_2_en = _T ? 1'h0 : _GEN_66;
  assign RegFile_MPORT_3_data = RegFile_MPORT_4_data;
  assign RegFile_MPORT_3_addr = 5'h8;
  assign RegFile_MPORT_3_mask = 1'h1;
  assign RegFile_MPORT_3_en = _T ? 1'h0 : _GEN_66;
  assign RegFile_MPORT_5_data = io_Status;
  assign RegFile_MPORT_5_addr = 5'hc;
  assign RegFile_MPORT_5_mask = 1'h1;
  assign RegFile_MPORT_5_en = _T ? 1'h0 : _GEN_66;
  assign RegFile_MPORT_6_data = {cause_hi,cause_lo};
  assign RegFile_MPORT_6_addr = 5'hd;
  assign RegFile_MPORT_6_mask = 1'h1;
  assign RegFile_MPORT_6_en = _T ? 1'h0 : _GEN_77;
  assign RegFile_MPORT_7_data = io_EPC;
  assign RegFile_MPORT_7_addr = 5'he;
  assign RegFile_MPORT_7_mask = 1'h1;
  assign RegFile_MPORT_7_en = _T ? 1'h0 : _GEN_77;
  assign RegFile_MPORT_8_data = io_BadVAddr;
  assign RegFile_MPORT_8_addr = 5'h8;
  assign RegFile_MPORT_8_mask = 1'h1;
  assign RegFile_MPORT_8_en = _T ? 1'h0 : _GEN_77;
  assign RegFile_MPORT_9_data = io_Status;
  assign RegFile_MPORT_9_addr = 5'hc;
  assign RegFile_MPORT_9_mask = 1'h1;
  assign RegFile_MPORT_9_en = _T ? 1'h0 : _GEN_77;
  assign io_dataout = io_CP0ctr == 4'h9 ? 32'h0 : _GEN_57; // @[CP0.scala 38:34 40:28]
  assign io_ExcHapp = io_CP0ctr == 4'h9 ? 1'h0 : _GEN_58; // @[CP0.scala 35:28 38:34]
  assign io_IF_ID_Flush = io_CP0ctr == 4'h9 ? 1'h0 : _GEN_58; // @[CP0.scala 31:32 38:34]
  assign io_ID_EX_Flush = io_CP0ctr == 4'h9 ? 1'h0 : _GEN_58; // @[CP0.scala 31:32 38:34]
  assign io_EX_MEM_Flush = io_CP0ctr == 4'h9 ? 1'h0 : _GEN_58; // @[CP0.scala 31:32 38:34]
  assign io_MEM_WB_Flush = 1'h0; // @[CP0.scala 34:33 38:34]
  always @(posedge clock) begin
    if (RegFile_MPORT_en & RegFile_MPORT_mask) begin
      RegFile[RegFile_MPORT_addr] <= RegFile_MPORT_data; // @[CP0.scala 28:26]
    end
    if (RegFile_MPORT_1_en & RegFile_MPORT_1_mask) begin
      RegFile[RegFile_MPORT_1_addr] <= RegFile_MPORT_1_data; // @[CP0.scala 28:26]
    end
    if (RegFile_MPORT_2_en & RegFile_MPORT_2_mask) begin
      RegFile[RegFile_MPORT_2_addr] <= RegFile_MPORT_2_data; // @[CP0.scala 28:26]
    end
    if (RegFile_MPORT_3_en & RegFile_MPORT_3_mask) begin
      RegFile[RegFile_MPORT_3_addr] <= RegFile_MPORT_3_data; // @[CP0.scala 28:26]
    end
    if (RegFile_MPORT_5_en & RegFile_MPORT_5_mask) begin
      RegFile[RegFile_MPORT_5_addr] <= RegFile_MPORT_5_data; // @[CP0.scala 28:26]
    end
    if (RegFile_MPORT_6_en & RegFile_MPORT_6_mask) begin
      RegFile[RegFile_MPORT_6_addr] <= RegFile_MPORT_6_data; // @[CP0.scala 28:26]
    end
    if (RegFile_MPORT_7_en & RegFile_MPORT_7_mask) begin
      RegFile[RegFile_MPORT_7_addr] <= RegFile_MPORT_7_data; // @[CP0.scala 28:26]
    end
    if (RegFile_MPORT_8_en & RegFile_MPORT_8_mask) begin
      RegFile[RegFile_MPORT_8_addr] <= RegFile_MPORT_8_data; // @[CP0.scala 28:26]
    end
    if (RegFile_MPORT_9_en & RegFile_MPORT_9_mask) begin
      RegFile[RegFile_MPORT_9_addr] <= RegFile_MPORT_9_data; // @[CP0.scala 28:26]
    end
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
`ifdef RANDOMIZE_MEM_INIT
  _RAND_0 = {1{`RANDOM}};
  for (initvar = 0; initvar < 32; initvar = initvar+1)
    RegFile[initvar] = _RAND_0[31:0];
`endif // RANDOMIZE_MEM_INIT
  `endif // RANDOMIZE
end // initial
`ifdef FIRRTL_AFTER_INITIAL
`FIRRTL_AFTER_INITIAL
`endif
`endif // SYNTHESIS
endmodule
