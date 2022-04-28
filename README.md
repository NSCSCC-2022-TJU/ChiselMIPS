使用Chisel实现的MIPS32 CPU
=======================

该CPU包含一个可选的TLB，已通过龙芯杯功能测试和《CPU设计实战》TLB功能测试。

## 指令
支持15条算术运算指令、8条逻辑运算指令，6条移位指令、12条分支跳转指令、4条数据移动指令、2条自陷指令、8条访存指令、6条特权指令，共计61条指令
- 算术运算指令：ADD, ADDI, ADDU, ADDIU, SUB, SUBU, SLT, SLTI, SLTU, SLTIU, MUL, DIV, DIVU, MULT, MULTU
- 逻辑运算指令：AND, ANDI, LUI, NOR, OR, ORI, XOR, XORI
- 移位指令：SLLV, SLL, SRAV, SRA, SRLV, SRL
- 分支跳转指令：BEQ, BNE, BGEZ, BGTZ, BLEZ, BLTZ, BGEZAL, BLTZAL, J, JAL, JR, JALR
- 数据移动指令：MFHI, MFLO, MTHI, MTLO
- 自陷指令：SYSCALL, BREAK
- 访存指令：LB, LH, LW, LBU, LHU, SB, SH, SW
- 特权指令: ERET, MFC0, MTC0, TLBP, TLBWI, TLBR
 
## 中断与例外
支持以下中断和例外
- 中断：硬件中断、软件中断、计时器中断
- 地址错例外-取指
- 保留指令例外
- 整形溢出例外
- 陷阱例外
- 系统调用例外
- 地址错例外-访存
- TLB重填例外
- TLB无效例外
- TLB修改例外

## 系统控制寄存器
CP0协处理器包含以下系统控制寄存器
- BadVaddr
- Count
- Status
- Cause
- EPC
- Compare
- Index
- EntryHi
- EntryLo0
- EntryLo1
- Config

## 使用说明
- 修改SysTop文件里SysTop对象的verilogFilePath之后，run该对象即可生成verilog文件
- verilog文件夹下包含Cache等硬件的verilog代码，需要手动拷贝至目标目录。