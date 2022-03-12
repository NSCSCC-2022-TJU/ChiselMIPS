circuit CMP :
  module CMP :
    input clock : Clock
    input reset : UInt<1>
    output io : { flip in1 : UInt<32>, flip in2 : UInt<32>, flip CMPctr : UInt<4>, CMPout : UInt<1>}

    node _T = eq(io.CMPctr, UInt<4>("h0")) @[CMP.scala 15:19]
    when _T : @[CMP.scala 15:30]
      node _io_CMPout_T = eq(io.in1, io.in2) @[CMP.scala 16:36]
      io.CMPout <= _io_CMPout_T @[CMP.scala 16:27]
    else :
      node _T_1 = eq(io.CMPctr, UInt<4>("h7")) @[CMP.scala 19:29]
      when _T_1 : @[CMP.scala 19:40]
        node _io_CMPout_T_1 = neq(io.in1, io.in2) @[CMP.scala 20:36]
        io.CMPout <= _io_CMPout_T_1 @[CMP.scala 20:27]
      else :
        node _T_2 = eq(io.CMPctr, UInt<4>("h1")) @[CMP.scala 23:29]
        when _T_2 : @[CMP.scala 23:40]
          node _io_CMPout_T_2 = asSInt(io.in1) @[CMP.scala 24:43]
          node _io_CMPout_T_3 = asSInt(io.in2) @[CMP.scala 24:59]
          node _io_CMPout_T_4 = lt(_io_CMPout_T_2, _io_CMPout_T_3) @[CMP.scala 24:45]
          io.CMPout <= _io_CMPout_T_4 @[CMP.scala 24:27]
        else :
          node _T_3 = eq(io.CMPctr, UInt<4>("h2")) @[CMP.scala 27:29]
          when _T_3 : @[CMP.scala 27:41]
            node _io_CMPout_T_5 = lt(io.in1, io.in2) @[CMP.scala 28:36]
            io.CMPout <= _io_CMPout_T_5 @[CMP.scala 28:27]
          else :
            node _T_4 = eq(io.CMPctr, UInt<4>("h5")) @[CMP.scala 31:23]
            when _T_4 : @[CMP.scala 31:35]
              node _io_CMPout_T_6 = asSInt(io.in1) @[CMP.scala 32:31]
              node _io_CMPout_T_7 = geq(_io_CMPout_T_6, asSInt(UInt<1>("h0"))) @[CMP.scala 32:34]
              io.CMPout <= _io_CMPout_T_7 @[CMP.scala 32:15]
            else :
              node _T_5 = eq(io.CMPctr, UInt<4>("h3")) @[CMP.scala 35:23]
              when _T_5 : @[CMP.scala 35:35]
                node _io_CMPout_T_8 = asSInt(io.in1) @[CMP.scala 36:31]
                node _io_CMPout_T_9 = gt(_io_CMPout_T_8, asSInt(UInt<1>("h0"))) @[CMP.scala 36:34]
                io.CMPout <= _io_CMPout_T_9 @[CMP.scala 36:15]
              else :
                node _T_6 = eq(io.CMPctr, UInt<4>("h6")) @[CMP.scala 39:23]
                when _T_6 : @[CMP.scala 39:35]
                  node _io_CMPout_T_10 = asSInt(io.in1) @[CMP.scala 40:31]
                  node _io_CMPout_T_11 = leq(_io_CMPout_T_10, asSInt(UInt<1>("h0"))) @[CMP.scala 40:34]
                  io.CMPout <= _io_CMPout_T_11 @[CMP.scala 40:15]
                else :
                  node _T_7 = eq(io.CMPctr, UInt<4>("ha")) @[CMP.scala 43:23]
                  when _T_7 : @[CMP.scala 43:35]
                    node _io_CMPout_T_12 = asSInt(io.in1) @[CMP.scala 44:31]
                    node _io_CMPout_T_13 = lt(_io_CMPout_T_12, asSInt(UInt<1>("h0"))) @[CMP.scala 44:34]
                    io.CMPout <= _io_CMPout_T_13 @[CMP.scala 44:15]
                  else :
                    io.CMPout <= UInt<1>("h0") @[CMP.scala 48:27]

