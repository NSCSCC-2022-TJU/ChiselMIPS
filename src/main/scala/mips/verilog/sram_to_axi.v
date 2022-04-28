`timescale 1ns / 1ps
//////////////////////////////////////////////////////////////////////////////////
// Company: 
// Engineer: 
// 
// Create Date: 2018/08/06 18:04:01
// Design Name: 
// Module Name: sram_to_axi
// Project Name: 
// Target Devices: 
// Tool Versions: 
// Description: 
// 
// Dependencies: 
// 
// Revision:
// Revision 0.01 - File Created
// Additional Comments:
// 
//////////////////////////////////////////////////////////////////////////////////
`include "defines.v"

module sram_to_axi(
    input  wire                                                         clk,
    input  wire                                                         resetn,  

    // 指令类SRAM接口                                   
    input  wire                                                         inst_req,        // 指令通道使能信号
    input  wire                                                         inst_uncached,  // 指令通道是否经过cache信号
    input  wire [`INST_ADDR_BUS]                                        inst_addr,      // 指令通道访存地址
    output wire                                                         inst_addr_ok,   // 地址有效信号，为1时可以通过总线进行访存操作
    output wire                                                         inst_beat_ok,
    output wire                                                         inst_data_ok,   // beat和data对应于burst机制中的单次访存完成信号和完成burst的完成信号
    output wire [`INST_BUS]                                             inst_rdata,     // 指令通道读数据

    // 数据类SRAM接口                                           
    input  wire                                                         data_rreq,
    input  wire                                                         data_wreq,        
    input  wire [`BSEL_BUS]                                             data_we,       
    input  wire                                                         data_runcached, 
    input  wire                                                         data_wuncached, 
    input  wire [`INST_ADDR_BUS]                                        data_raddr,     
    input  wire [`INST_ADDR_BUS]                                        data_waddr,  
    output wire [`INST_BUS]                                             data_rdata,    
    input  wire [`DATA_WIDTH * (2 ** (`OFFSET_WIDTH - 2)) - 1 : 0]      data_wdata,    
    output wire                                                         data_raddr_ok, 
    output wire                                                         data_waddr_ok,  
    output wire                                                         data_beat_ok,
    output wire                                                         data_data_ok,  

    // AXI接口
    // 读地址通道信号
    output [3 :0]                                                       arid         ,
    output [31:0]                                                       araddr       ,
    output [7 :0]                                                       arlen        ,
    output [2 :0]                                                       arsize       ,
    output [1 :0]                                                       arburst      ,
    output [1 :0]                                                       arlock       ,    //ignored
    output [3 :0]                                                       arcache      ,    //ignored
    output [2 :0]                                                       arprot       ,    //ignored
    output                                                              arvalid      ,
    input                                                               arready      ,
    // 读数据通道信号         
    input  [3 :0]                                                       rid          ,    //ignored
    input  [31:0]                                                       rdata        ,
    input  [1 :0]                                                       rresp        ,    //ignored
    input                                                               rlast        ,
    input                                                               rvalid       ,
    output                                                              rready       ,
    // 写地址通道信号                                       
    output [3 :0]                                                       awid         ,
    output [31:0]                                                       awaddr       ,
    output [7 :0]                                                       awlen        ,
    output [2 :0]                                                       awsize       ,
    output [1 :0]                                                       awburst      ,
    output [1 :0]                                                       awlock       ,    //ignored
    output [3 :0]                                                       awcache      ,    //ignored
    output [2 :0]                                                       awprot       ,    //ignored
    output                                                              awvalid      ,
    input                                                               awready      ,    
    // 写数据通道信号                                           
    output [3 :0]                                                       wid          , 
    output reg [31:0]                                                   wdata        ,
    output [3 :0]                                                       wstrb        ,
    output reg                                                          wlast        ,
    output reg                                                          wvalid       ,
    input                                                               wready       ,
    // 写响应通道信号                                               
    input  [3 :0]                                                       bid          ,    //ignored
    input  [1 :0]                                                       bresp        ,    //ignored
    input                                                               bvalid       ,
    output                                                              bready       

    );

    parameter NUM_INST_PER_LINE     = (2 ** (`OFFSET_WIDTH - 2)); //每行的指令数目
    parameter DATA_BLOCK_WIDTH      = `DATA_WIDTH * NUM_INST_PER_LINE;

    //read
    reg [1 : 0]                         read_state;

    parameter                           READ_STATE_FREE     = 0;
    parameter                           READ_STATE_REQ      = 1;
    parameter                           READ_STATE_READ     = 2;

    reg [`INST_ADDR_BUS]                read_buffer_addr;
    reg                                 read_buffer_uncached;
    reg                                 read_buffer_data_inst;

    wire                                allow_new_read;
    wire                                beat_back;
    wire                                data_back;

    //write 
    reg [1 : 0]                         write_state;

    parameter                           WRITE_STATE_FREE    = 0;
    parameter                           WRITE_STATE_REQ     = 1;
    parameter                           WRITE_STATE_WRITE   = 2;

    reg [`INST_ADDR_BUS]                write_buffer_addr;
    reg                                 write_buffer_uncached;
    reg [DATA_BLOCK_WIDTH - 1 : 0]      write_buffer_data;
    reg [3 : 0]                         write_buffer_we;
    reg [(`OFFSET_WIDTH - 2) : 0]       write_buffer_cnt;

    wire                                allow_new_write;

    wire                                read_write_conflict;

    wire [1 : 0]                        size;


    assign size = write_buffer_we == 4'b1111 ? 2'b10 :
                    write_buffer_we == 4'b1100 || write_buffer_we == 4'b0011 ? 2'b01 : 2'b0;


    assign read_write_conflict = read_buffer_addr == data_waddr && read_state != READ_STATE_FREE;

    assign allow_new_read = read_state == READ_STATE_FREE || read_state == READ_STATE_READ && rlast;

    assign allow_new_write = write_state == WRITE_STATE_FREE || write_state == WRITE_STATE_WRITE && bvalid;
    //更新read_state
    always @(posedge clk) begin
        if (resetn == `RST_ENABLE) begin
           read_state <= READ_STATE_FREE;
        end
        else
            case (read_state)
                READ_STATE_FREE : begin
                    read_state <= inst_req || data_rreq ? READ_STATE_REQ : READ_STATE_FREE;
                end
                READ_STATE_REQ : begin
                    read_state <= arready ? READ_STATE_READ : READ_STATE_REQ;
                end
                READ_STATE_READ : begin
                    read_state <= !rlast ? READ_STATE_READ :
                                    inst_req || data_rreq ? READ_STATE_REQ : READ_STATE_FREE;
                end
            endcase
    end

    //更新read_buffer
    always @(posedge clk) begin
        if (allow_new_read) begin
            read_buffer_addr <= data_rreq ? data_raddr : inst_addr;
            read_buffer_uncached <= data_rreq ? data_runcached : inst_uncached;
            read_buffer_data_inst <= data_rreq;
        end
    end

    //更新write_state
    always @(posedge clk) begin
        if (resetn == `RST_ENABLE) begin
           write_state <= WRITE_STATE_FREE;
        end
        else
            case (write_state)
                WRITE_STATE_FREE : begin
                    write_state <=  read_write_conflict ? WRITE_STATE_FREE :
                                    data_wreq ? WRITE_STATE_REQ : WRITE_STATE_FREE;
                end
                WRITE_STATE_REQ : begin
                    write_state <= awready ? WRITE_STATE_WRITE : WRITE_STATE_REQ;
                end
                WRITE_STATE_WRITE : begin
                    write_state <= !bvalid ? WRITE_STATE_WRITE :
                                    read_write_conflict ? WRITE_STATE_FREE :
                                    data_wreq ? WRITE_STATE_REQ : WRITE_STATE_FREE;
                end
            endcase
    end

    //更新write_buffer
    always @(posedge clk) begin
        if (allow_new_write) begin
            write_buffer_addr <= data_waddr;
            write_buffer_uncached <= data_wuncached;
            write_buffer_data <= data_wdata;
            write_buffer_we <= data_we;
            write_buffer_cnt <= 0;
        end
        else if (write_state == WRITE_STATE_WRITE && !write_buffer_uncached && write_buffer_cnt < NUM_INST_PER_LINE && wready) begin
            write_buffer_cnt <= write_buffer_cnt + 1;
        end
        else if (write_state == WRITE_STATE_WRITE && write_buffer_uncached && write_buffer_cnt < 1 && wready) begin
            write_buffer_cnt <= write_buffer_cnt + 1;
        end
    end

    always @(*) begin
        wdata = write_buffer_data[(write_buffer_cnt + 1) * `DATA_WIDTH - 1 -: `DATA_WIDTH];
        wvalid = 0;
        wlast = 0;
        if (write_state == WRITE_STATE_WRITE && !write_buffer_uncached && write_buffer_cnt < NUM_INST_PER_LINE) begin
            wvalid = 1;
            wlast = write_buffer_cnt == NUM_INST_PER_LINE - 1;
        end
        else if (write_state == WRITE_STATE_WRITE && write_buffer_uncached && write_buffer_cnt < 1) begin
            wvalid = 1;
            wlast = 1;
        end
    end
    

    assign beat_back = read_state == READ_STATE_READ && rvalid && rready;
    assign data_back = read_state == READ_STATE_READ && rvalid && rready && rlast;

    assign inst_addr_ok = allow_new_read && !data_rreq;
    assign inst_rdata = rdata;
    assign inst_beat_ok = !read_buffer_data_inst && beat_back;
    assign inst_data_ok = !read_buffer_data_inst && data_back;
    assign data_raddr_ok = allow_new_read;
    assign data_rdata = rdata;
    assign data_beat_ok = read_buffer_data_inst && beat_back;
    assign data_data_ok = read_buffer_data_inst && data_back;
    assign data_waddr_ok = allow_new_write && !read_write_conflict;

    // 设置AXI相关信号
    // 读地址通道信号
    assign arid    = 4'd0;
    assign araddr  = read_buffer_addr;
    assign arlen   = read_buffer_uncached ? 0 : NUM_INST_PER_LINE - 1;
    assign arsize  = 2'b10;  
    assign arburst = read_buffer_uncached ? 0 : 1;
    assign arlock  = 2'd0;
    assign arcache = 4'd0;
    assign arprot  = 3'd0;
    assign arvalid = read_state == READ_STATE_REQ;
    // 读数据通道信号
    assign rready  = 1'b1;
    // 写地址通道信号
    assign awid    = 4'd0;
    assign awaddr  = write_buffer_addr;
    assign awlen   = write_buffer_uncached ? 0 : NUM_INST_PER_LINE - 1;
    assign awsize  = write_buffer_uncached ? size : 2'b10; 
    assign awburst = write_buffer_uncached ? 0 : 1;
    assign awlock  = 2'd0;
    assign awcache = 4'd0;
    assign awprot  = 3'd0;
    assign awvalid = write_state == WRITE_STATE_REQ;
    //写数据通道信号
    assign wid    = 4'd0;
    assign wstrb  = write_buffer_uncached ? write_buffer_we : 4'b1111;
    //写响应通道信号
    assign bready  = 1'b1;

endmodule
