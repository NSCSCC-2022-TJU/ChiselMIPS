circuit DataCheck :
  module DataCheck :
    input clock : Clock
    input reset : UInt<1>
    output io : { flip ID_EX_Rs : UInt<5>, flip ID_EX_Rt : UInt<5>, flip Ex_Mem_Rd : UInt<5>, flip Mem_WB_Rd : UInt<5>, flip Ex_Mem_RegWr : UInt<1>, flip Mem_WB_RegWr : UInt<1>, ForwardA : UInt<2>, ForwardB : UInt<2>}

    node _T = neq(io.Ex_Mem_Rd, UInt<1>("h0")) @[DATACHECK.scala 18:54]
    node _T_1 = and(io.Ex_Mem_RegWr, _T) @[DATACHECK.scala 18:38]
    node _T_2 = eq(io.Ex_Mem_Rd, io.ID_EX_Rs) @[DATACHECK.scala 18:78]
    node _T_3 = and(_T_1, _T_2) @[DATACHECK.scala 18:62]
    when _T_3 : @[DATACHECK.scala 18:94]
      io.ForwardA <= UInt<2>("h2") @[DATACHECK.scala 19:37]
    else :
      node _T_4 = neq(io.Mem_WB_Rd, UInt<1>("h0")) @[DATACHECK.scala 20:60]
      node _T_5 = and(io.Mem_WB_RegWr, _T_4) @[DATACHECK.scala 20:44]
      node _T_6 = eq(io.Mem_WB_Rd, io.ID_EX_Rs) @[DATACHECK.scala 20:84]
      node _T_7 = and(_T_5, _T_6) @[DATACHECK.scala 20:68]
      when _T_7 : @[DATACHECK.scala 20:100]
        io.ForwardA <= UInt<2>("h1") @[DATACHECK.scala 21:37]
      else :
        io.ForwardA <= UInt<2>("h0") @[DATACHECK.scala 23:37]
    node _T_8 = neq(io.Ex_Mem_Rd, UInt<1>("h0")) @[DATACHECK.scala 26:54]
    node _T_9 = and(io.Ex_Mem_RegWr, _T_8) @[DATACHECK.scala 26:38]
    node _T_10 = eq(io.Ex_Mem_Rd, io.ID_EX_Rt) @[DATACHECK.scala 26:78]
    node _T_11 = and(_T_9, _T_10) @[DATACHECK.scala 26:62]
    when _T_11 : @[DATACHECK.scala 26:94]
      io.ForwardB <= UInt<2>("h2") @[DATACHECK.scala 27:37]
    else :
      node _T_12 = neq(io.Mem_WB_Rd, UInt<1>("h0")) @[DATACHECK.scala 28:60]
      node _T_13 = and(io.Mem_WB_RegWr, _T_12) @[DATACHECK.scala 28:44]
      node _T_14 = eq(io.Mem_WB_Rd, io.ID_EX_Rt) @[DATACHECK.scala 28:84]
      node _T_15 = and(_T_13, _T_14) @[DATACHECK.scala 28:68]
      when _T_15 : @[DATACHECK.scala 28:100]
        io.ForwardB <= UInt<2>("h1") @[DATACHECK.scala 29:37]
      else :
        io.ForwardB <= UInt<2>("h0") @[DATACHECK.scala 31:37]

