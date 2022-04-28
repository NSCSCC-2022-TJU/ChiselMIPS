`timescale 1ns / 1ps

/*------------------- 全局参数 -------------------*/
`define RST_ENABLE      1'b0                // 复位信号有效  RST_ENABLE
`define RST_DISABLE     1'b1                // 复位信号无效
`define ZERO_BYTE       8'h00               // 8位的数值0(add)
`define ZERO_HWORD      16'h0000            // 16位的数值0(add)
`define ZERO_WORD       32'h00000000        // 32位的数值0
`define ZERO_DWORD      64'b0               // 64位的数值0
`define ONE_WORD        32'h00000001        // 32位的数值1(add)
`define WRITE_ENABLE    1'b1                // 使能写
`define WRITE_DISABLE   1'b0                // 禁止写
`define READ_ENABLE     1'b1                // 使能读
`define READ_DISABLE    1'b0                // 禁止读
`define ALUOP_BUS       7 : 0               // 译码阶段的输出aluop_o的宽度
`define SHIFT_ENABLE    1'b1                // 移位指令使能
`define ALUTYPE_BUS     2 : 0               // 译码阶段的输出alutype_o的宽度
`define TRUE_V          1'b1                // 逻辑"真"
`define FALSE_V         1'b0                // 逻辑"假"
`define CHIP_ENABLE     1'b1                // 芯片使能
`define CHIP_DISABLE    1'b0                // 芯片禁止
`define WORD_BUS        31: 0               // 32位宽
`define DOUBLE_REG_BUS  63: 0               // 两倍的通用寄存器的数据线宽度
`define HI_ADDR         63: 32              // hi(add)
`define LO_ADDR         31: 0               // lo(add)
`define RT_ENABLE       1'b1                // rt选择使能
`define SIGNED_EXT      1'b1                // 符号扩展使能
`define IMM_ENABLE      1'b1                // 立即数选择使能
`define UPPER_ENABLE    1'b1                // 立即数逻辑左移16位使能
`define MREG_ENABLE     1'b1                // 写回阶段存储器结果选择信号
`define BSEL_BUS        3 : 0               // 数据存储器字节选择信号宽度
`define PC_INIT         32'hbfc00000        // PC初始值
`define JUMP_BUS        25:0                //J型指令instr_index字段的宽度（add）          
`define JTSEL_BUS       1:0                 //转移地址选择信号的宽度（add）

/*------------------- Cache参数 -------------------*/
`define TAG_WIDTH       26 //20
`define INDEX_WIDTH     2 //8
`define OFFSET_WIDTH    4 //4
`define INST_NUM_ASSOC  2
`define DATA_NUM_ASSOC  2
`define DATA_WIDTH      32

/*------------------- 指令字参数 -------------------*/
`define INST_ADDR_BUS   31: 0               // 指令的地址宽度
`define INST_BUS        31: 0               // 指令的数据宽度
`define DATA_ADDR_BUS   31:0
`define DATA_BUS        31:0

// 操作类型alutype
// `define NOP             3'b000
`define ARITH           3'b001
`define LOGIC           3'b010
`define MOVE            3'b011
`define SHIFT           3'b100
`define JUMP            3'b101
`define MUL             3'b110

// 内部操作码aluop
`define MINIMIPS32_ADD      8'h20
`define MINIMIPS32_SUBU     8'h21
`define MINIMIPS32_SLT      8'h22
`define MINIMIPS32_AND      8'h40
`define MINIMIPS32_MULT     8'h00
`define MINIMIPS32_MFHI     8'h60
`define MINIMIPS32_MFLO     8'h61
`define MINIMIPS32_SLL      8'h80
`define MINIMIPS32_ADDU     8'h23
`define MINIMIPS32_SUB      8'h24
`define MINIMIPS32_SLTU     8'h25
`define MINIMIPS32_OR       8'h41
`define MINIMIPS32_NOR      8'h42
`define MINIMIPS32_XOR      8'h43
`define MINIMIPS32_SRL      8'h81
`define MINIMIPS32_SRA      8'h82
`define MINIMIPS32_SLLV     8'h83
`define MINIMIPS32_SRLV     8'h84
`define MINIMIPS32_SRAV     8'h85
`define MINIMIPS32_MULTU    8'h01
`define MINIMIPS32_DIV      8'h02
`define MINIMIPS32_DIVU     8'h03
`define MINIMIPS32_MTHI     8'h62
`define MINIMIPS32_MTLO     8'h63

`define MINIMIPS32_ADDIU    8'h26
`define MINIMIPS32_SLTIU    8'h27
`define MINIMIPS32_ORI      8'h44
`define MINIMIPS32_LUI      8'h45
`define MINIMIPS32_LB       8'h28
`define MINIMIPS32_LW       8'h29
`define MINIMIPS32_SB       8'h2A
`define MINIMIPS32_SW       8'h2B
`define MINIMIPS32_ADDI     8'h2C
`define MINIMIPS32_SLTI     8'h2D
`define MINIMIPS32_ANDI     8'h46
`define MINIMIPS32_XORI     8'h47
`define MINIMIPS32_LBU      8'h2E
`define MINIMIPS32_LH       8'h2F
`define MINIMIPS32_LHU      8'h30
`define MINIMIPS32_SH       8'h31

`define MINIMIPS32_J        8'h04
`define MINIMIPS32_JAL      8'hA0
`define MINIMIPS32_JR       8'h05
`define MINIMIPS32_BEQ      8'h06
`define MINIMIPS32_BNE      8'h07
`define MINIMIPS32_BGEZ     8'h08
`define MINIMIPS32_BGTZ     8'h09
`define MINIMIPS32_BLEZ     8'h0A
`define MINIMIPS32_BLTZ     8'h0B
`define MINIMIPS32_BGEZAL   8'hA1
`define MINIMIPS32_BLTZAL   8'hA2
`define MINIMIPS32_JALR     8'hA3

`define MINIMIPS32_MFC0     8'h64
`define MINIMIPS32_MTC0     8'h0C
`define MINIMIPS32_SYSCALL  8'h0D
`define MINIMIPS32_ERET     8'h0E
`define MINIMIPS32_BREAK    8'h0F


/*------------------- 通用寄存器堆参数 -------------------*/
`define REG_BUS         31: 0               // 寄存器数据宽度
`define REG_ADDR_BUS    4 : 0               // 寄存器的地址宽度
`define REG_NUM         32                  // 寄存器数量32个
`define REG_NOP         5'b00000            // 零号寄存器


/*------------------- 流水线暂停参数 -------------------*/
`define STALL_BUS       5:0                 //暂停信号宽度
`define STOP            1'b1                //暂停信号
`define NOSTOP          1'b0                //不暂停信号


/*------------------- 除法指令参数 -------------------*/
`define DIV_FREE        2'b00                //除法准备状态
`define DIV_BY_ZERO     2'b01                //除法是否除零状态
`define DIV_ON          2'b10                //除法开始状态
`define DIV_END         2'b11                //除法结束状态
`define DIV_READY       1'b1                 //除法运算结束状态
`define DIV_NOT_READY   1'b0                 //除法运算未结束状态
`define DIV_START       1'b1                 //除法开始信号
`define DIV_STOP        1'b0                 //除法未开始信号



/*------------------- 异常处理参数 -------------------*/
//CP0协处理器参数
`define CP0_INT_BUS     5:0         //中断信号宽度
`define CP0_BADVADDR    8           //BADVADDR寄存器地址
`define CP0_COUNT       9           //COUNT寄存器地址
`define CP0_STATUS      12          //STATUS寄存器地址
`define CP0_CAUSE       13          //CAUSE寄存器地址
`define CP0_EPC         14          //EPC寄存器地址

//异常处理参数
`define EXC_CODE_BUS    4:0         //异常类型编码宽度
`define EXC_INT         5'b00       //中断异常
`define EXC_SYS         5'h08       //系统调用异常
`define EXC_OV          5'h0C       //整数溢出异常
`define EXC_NONE        5'h10       //无异常
`define EXC_ERET        5'h11       //ERET异常
`define EXC_ADDR       32'hbfc00380 // 异常处理程序入口地址
`define EXC_INT_ADDR   32'hbfc00380 // 中断异常处理程序入口地址
//异常处理参数（添加）
`define EXC_ADEL        5'h04       //取址/加载地址错误异常
`define EXC_RI          5'h0A       //保留指令异常
`define EXC_ADES        5'h05       //存储地址错误异常
`define EXC_BREAK       5'h09       //Break异常的编码
`define NOFLUSH         1'b0        //不清空流水线
`define FLUSH           1'b1        //清空流水线



// /*------------------- 设备参数 -------------------*/
// `define IO_ADDR_BASE    16'hBFD0     //异常处理程序入口地址

