module AND(
  input   clock,
  input   reset,
  input   io_a,
  input   io_b,
  output  io_c
);
  assign io_c = io_a & io_b; // @[AND.scala 13:16]
endmodule
