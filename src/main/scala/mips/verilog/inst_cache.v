`include "defines.v"
/*
Cache size : 8KB
Block size : 16B
Associate : 2
Line number : 256
*/

/*
行为模式：cpu_req为0，cache持续输出上一次的结果
*/

module inst_cache #(parameter
	TAG_WIDTH = `TAG_WIDTH,
	INDEX_WIDTH = `INDEX_WIDTH,
	OFFSET_WIDTH = `OFFSET_WIDTH,
	NUM_ASSOC = `INST_NUM_ASSOC,
    DATA_ADDR_BUS = 32,
    DATA_BUS = 32
) (
	// clock and reset
	input  wire 					clk,
	input  wire 					rst,

	//与MiniMIPS32处理器核互连的信号
    input  wire 					cpu_req,
    input  wire [`INST_ADDR_BUS	]   cpu_addr,
    input  wire                     cpu_uncached,
	
    output wire                     cpu_addr_ok,
    output wire                     cpu_operation_ok,
    output reg  [`INST_BUS		]   cpu_rdata,

	//类sram接口
    output wire [3   :   0  	]	ram_req,
    output wire 				    ram_uncached,
    output wire [`INST_ADDR_BUS	]   ram_addr,

    input  wire 					ram_addr_ok,
	input  wire						ram_beat_ok,
    input  wire 					ram_data_ok,
    input  wire [`INST_BUS		]   ram_rdata
);

    parameter NUM_CACHE_LINES       = (2 ** INDEX_WIDTH); //Cache行数
    parameter NUM_INST_PER_LINE     = (2 ** (OFFSET_WIDTH - 2)); //每行的指令数目
    parameter DATA_BLOCK_WIDTH      = DATA_BUS * NUM_INST_PER_LINE;
    parameter ASSOC_WIDTH           = $clog2(NUM_ASSOC);

    //CPU接口声明
    wire [INDEX_WIDTH - 1 : 0] cpu_addr_index;
    wire [OFFSET_WIDTH - 3 : 0] cpu_addr_offset;

    assign cpu_addr_index = cpu_addr[OFFSET_WIDTH + INDEX_WIDTH - 1: OFFSET_WIDTH];
    assign cpu_addr_offset = cpu_addr[OFFSET_WIDTH - 1 : 2];

    //RAM接口声明
    reg [NUM_ASSOC-1:0]                             data_ram_we;
    wire [INDEX_WIDTH-1:0]                          data_ram_addr;
    wire [DATA_BLOCK_WIDTH-1:0]                     data_ram_dout [NUM_ASSOC-1:0];
    wire [DATA_BLOCK_WIDTH-1:0]                     data_ram_din;

    reg [NUM_ASSOC-1:0]                             tagv_ram_we;
    wire [INDEX_WIDTH-1:0]                          tagv_ram_addr;
    wire [TAG_WIDTH:0]                              tagv_ram_dout [NUM_ASSOC-1:0];
    wire [TAG_WIDTH:0]                              tagv_ram_din;

    //CACHE_STATE声明
    reg [1:0] cache_state;

    parameter STATE_FREE            = 0;
    parameter STATE_LOOK_UP         = 1;
    parameter STATE_RAM_REQ         = 2;
    parameter STATE_UPDATE          = 3;
    // parameter STATE_UPDATE_COMPLETE = 4;

    wire allow_new_req;

    //REQUEST_BUFFER声明
        //REQUEST_BUFFER = {CPU_ADDR, CPU_UNCACHED}
    reg [DATA_ADDR_BUS - 1 : 0]                     request_buffer_addr; 
    reg                                             request_buffer_uncached;

    wire [TAG_WIDTH - 1 : 0]                        request_buffer_addr_tag;
    wire [INDEX_WIDTH - 1 : 0]                      request_buffer_addr_index;
    wire [OFFSET_WIDTH - 3 : 0]                     request_buffer_addr_offset;

    //UPDATE_BUFFER声明
        //UPDATE_BUFFER = {UPDATE_BUFFER_ADDR, UPDATE_BUFFER_VALID, UPDATE_BUFFER_DATA}

    reg [DATA_ADDR_BUS - 1 : 0]                     update_buffer_addr;
    reg [NUM_INST_PER_LINE - 1 : 0]                 update_buffer_valid;
    reg [DATA_BLOCK_WIDTH - 1 : 0]                  update_buffer_data;
    reg [ASSOC_WIDTH - 1 : 0]                       update_buffer_way;
    reg                                             update_buffer_uncached;
    
    wire [INDEX_WIDTH - 1 : 0]                      update_buffer_addr_index;
    wire [TAG_WIDTH - 1 : 0]                        update_buffer_addr_tag;

    //lookup声明
    reg                                             hit;
    reg [ASSOC_WIDTH - 1 : 0]                       hit_way;
    reg [ASSOC_WIDTH : 0]                           free_way; //free_way = {free_index, valid}
    reg                                             ram_req_flag;//指示ram请求何时停止

    //update声明
    reg [(OFFSET_WIDTH - 2) - 1 : 0]                update_cnt;

    wire [TAG_WIDTH : 0]                            update_tagv;
    wire [DATA_BLOCK_WIDTH - 1 : 0]                 update_data;
    wire [INDEX_WIDTH - 1 : 0]                      update_index;

    //LFSR声明
    reg [7 : 0]                                     lfsr;           //位宽越大，越随机，低位宽，0的概率会较低

    always @(posedge clk) begin
        if (rst == `RST_ENABLE) lfsr <= 3'b111;
        else lfsr <= {lfsr[6], lfsr[5] ^ lfsr[7], lfsr[4] ^ lfsr[7], lfsr[3] ^ lfsr[7], lfsr[2], lfsr[1], lfsr[0], lfsr[7]};
    end


    //ram接口连线
    assign data_ram_addr    = allow_new_req && cpu_req ? cpu_addr_index :
                                data_ram_we == 0 ? request_buffer_addr_index : update_index;
    assign tagv_ram_addr    = allow_new_req && cpu_req ? cpu_addr_index :
                                data_ram_we == 0 ? request_buffer_addr_index : update_index;
    assign data_ram_din     = update_data;
    assign tagv_ram_din     = update_tagv;


    integer i;

    always @(*) begin 
        for (i=0; i<NUM_ASSOC; i=i+1) begin
            data_ram_we[i] = cache_state != STATE_UPDATE ? 0 :
                        !ram_data_ok ? 0 :
                        update_buffer_uncached ? 0 :
                        i == update_buffer_way ? 1 : 0;

            tagv_ram_we[i] = cache_state != STATE_UPDATE ? 0 :
                        !ram_data_ok ? 0 :
                        update_buffer_uncached ? 0 :
                        i == update_buffer_way ? 1 : 0;
        end
    end

    //生成数据ram
    generate
        genvar m;
        for (m=0; m<NUM_ASSOC; m=m+1) begin:DATA_RAM
            xpm_memory_spram #(
                .AUTO_SLEEP_TIME(0),           // ignore
                .CASCADE_HEIGHT(0),            // ignore
                .ECC_MODE("no_ecc"),           // ignore
                .MEMORY_INIT_FILE("none"),     // ignore
                .MEMORY_INIT_PARAM("0"),       // 初始化填充内容
                .MEMORY_OPTIMIZATION("true"),  // ？？？
                .MESSAGE_CONTROL(0),           // ignore
                .MEMORY_PRIMITIVE("auto"),    // block ram
                .RST_MODE_A("SYNC"),           // 同步复位
                .WAKEUP_TIME("disable_sleep"), // ignore
                .USE_MEM_INIT(1),              // 生成时显示ram初始值
                .READ_LATENCY_A(1),            // block ram设为1
                .SIM_ASSERT_CHK(1),            // 是否汇报仿真信息，默认为0；取个1看看有什么用？
                .READ_RESET_VALUE_A("0"),      // rst时，read返回值
                .WRITE_MODE_A("write_first"),  // 写优先

                .ADDR_WIDTH_A(INDEX_WIDTH),              // 地址宽度
                .BYTE_WRITE_WIDTH_A(DATA_BLOCK_WIDTH),       // 字节宽度；若不启用写字节使能则与WRITE_DATA_WIDTH_A一致
                .READ_DATA_WIDTH_A(DATA_BLOCK_WIDTH),        // The values of READ_DATA_WIDTH_A and WRITE_DATA_WIDTH_A must be equal.
                .WRITE_DATA_WIDTH_A(DATA_BLOCK_WIDTH),       // The values of READ_DATA_WIDTH_A and WRITE_DATA_WIDTH_A must be equal.
                .MEMORY_SIZE(NUM_CACHE_LINES * DATA_BLOCK_WIDTH)            // (2**addr_width)*data_width
            )
            data (
                .dbiterra(),                     // 指示错误  output
                .sbiterra(),                     // 指示错误  output  
                .injectdbiterra(0),              // ecc input
                .injectsbiterra(0),              // ecc input
                .regcea(0),                      // input: Clock Enable for the last register stage on the output data path.
                .sleep(0),                       // 1-bit input: sleep signal to enable the dynamic power saving feature.

                .ena(1),                       
                .clka(clk),                    
                .rsta(),                     
                .addra(data_ram_addr),                   
                .wea(data_ram_we[m]),                        // 当word-wide write时，为1bit，否则与字节宽度有关；例如32bits宽度需要4bits input
                .douta(data_ram_dout[m]),                   
                .dina(data_ram_din)                    
            );
        end
    endgenerate

    //生成tagv ram
    generate
        genvar n;
        for (n=0; n<NUM_ASSOC; n=n+1) begin:TAGV_RAM
            xpm_memory_spram #(
                .AUTO_SLEEP_TIME(0),           // ignore
                .CASCADE_HEIGHT(0),            // ignore
                .ECC_MODE("no_ecc"),           // ignore
                .MEMORY_INIT_FILE("none"),     // ignore
                .MEMORY_INIT_PARAM("0"),       // 初始化填充内容
                .MEMORY_OPTIMIZATION("true"),  // ？？？
                .MESSAGE_CONTROL(0),           // ignore
                .MEMORY_PRIMITIVE("auto"),    // block ram
                .RST_MODE_A("SYNC"),           // 同步复位
                .WAKEUP_TIME("disable_sleep"), // ignore
                .USE_MEM_INIT(1),              // 生成时显示ram初始值
                .READ_LATENCY_A(1),            // block ram设为1
                .SIM_ASSERT_CHK(1),            // 是否汇报仿真信息，默认为0；取个1看看有什么用？
                .READ_RESET_VALUE_A("0"),      // rst时，read返回值
                .WRITE_MODE_A("write_first"),  // 写优先

                .ADDR_WIDTH_A(INDEX_WIDTH),              // 地址宽度
                .BYTE_WRITE_WIDTH_A(TAG_WIDTH + 1),       // 字节宽度；若不启用写字节使能则与WRITE_DATA_WIDTH_A一致
                .READ_DATA_WIDTH_A(TAG_WIDTH + 1),        // The values of READ_DATA_WIDTH_A and WRITE_DATA_WIDTH_A must be equal.
                .WRITE_DATA_WIDTH_A(TAG_WIDTH + 1),       // The values of READ_DATA_WIDTH_A and WRITE_DATA_WIDTH_A must be equal.
                .MEMORY_SIZE(NUM_CACHE_LINES * (TAG_WIDTH + 1))            // (2**addr_width)*data_width
            )
            tagv (
                .dbiterra(),                     // 指示错误  output
                .sbiterra(),                     // 指示错误  output  
                .injectdbiterra(0),              // ecc input
                .injectsbiterra(0),              // ecc input
                .regcea(0),                      // input: Clock Enable for the last register stage on the output data path.
                .sleep(0),                       // 1-bit input: sleep signal to enable the dynamic power saving feature.

                .ena(1),                       
                .clka(clk),                    
                .rsta(),                     
                .addra(tagv_ram_addr),                   
                .wea(tagv_ram_we[n]),                        // 当word-wide write时，为1bit，否则与字节宽度有关；例如32bits宽度需要4bits input
                .douta(tagv_ram_dout[n]),                   
                .dina(tagv_ram_din)                     
            );
        end
    endgenerate
    

    
    //request_buffer连线
    assign request_buffer_addr_tag      = request_buffer_addr[DATA_ADDR_BUS - 1 : INDEX_WIDTH + OFFSET_WIDTH];
    assign request_buffer_addr_index    = request_buffer_addr[(INDEX_WIDTH + OFFSET_WIDTH) - 1 : OFFSET_WIDTH];
    assign request_buffer_addr_offset   = request_buffer_addr[OFFSET_WIDTH - 1 : 2];

    //更新requst_buffer
    always @(posedge clk) begin
        if (rst == `RST_ENABLE) begin
            request_buffer_addr     <= 0;
            request_buffer_uncached <= 0;
        end
        else if (allow_new_req && cpu_req) begin
            request_buffer_addr     <= cpu_addr;  //解决取指地址非对齐异常
            request_buffer_uncached <= cpu_uncached;
        end
    end

    //lookup连线
    // assign allow_new_req = (cache_state == STATE_FREE) || ((cache_state == STATE_LOOK_UP) && hit) || ((cache_state == STATE_RAM_REQ) && hit) || (cache_state == STATE_UPDATE && update_buffer_uncached && ram_data_ok);
    assign allow_new_req = cache_state == STATE_FREE ? 1 :
                            cache_state != STATE_UPDATE ? hit :
                            update_buffer_uncached && ram_data_ok ? 1 : 0;
    
    assign cpu_addr_ok = allow_new_req;

    always @(*) begin
        hit = 0;
        hit_way = 0;
        cpu_rdata = 0;
        free_way = 0;
        if (request_buffer_addr[1:0] != 0) begin
            hit = 1;
        end 
        else if (!request_buffer_uncached) begin
            for (i=0; i<NUM_ASSOC; i=i+1) begin
                if (tagv_ram_dout[i][0]) begin
                    if (tagv_ram_dout[i][TAG_WIDTH : 1] == request_buffer_addr_tag) begin
                        hit = 1;
                        hit_way = i;
                        cpu_rdata = data_ram_dout[i][(request_buffer_addr_offset + 1) * DATA_BUS - 1 -: DATA_BUS];
                    end
                end
                else begin
                    free_way = {i[ASSOC_WIDTH - 1 : 0], 1'd1};
                end
            end
            if (!update_buffer_uncached) begin
                if ({request_buffer_addr_tag, request_buffer_addr_index} == {update_buffer_addr_tag, update_buffer_addr_index} && update_buffer_valid[request_buffer_addr_offset]) begin
                    hit = 1;
                    hit_way = update_buffer_way;
                    cpu_rdata = update_buffer_data[(request_buffer_addr_offset + 1) * DATA_BUS - 1 -: DATA_BUS];
                end
                else if ({request_buffer_addr_tag, request_buffer_addr_index} == {update_buffer_addr_tag, update_buffer_addr_index} && update_cnt == request_buffer_addr_offset && ram_beat_ok) begin
                    hit = 1;
                    hit_way = update_buffer_way;
                    cpu_rdata = ram_rdata;
                end
            end
        end
        else begin
            if (request_buffer_addr == update_buffer_addr && update_buffer_uncached && update_buffer_valid[0]) begin
                hit = 1;
                cpu_rdata = update_buffer_data[DATA_BUS - 1 : 0];
            end
            else if (request_buffer_addr == update_buffer_addr && update_buffer_uncached && ram_data_ok) begin
                hit = 1;
                cpu_rdata = ram_rdata;
            end
        end

    end


    //update_buffer连线
    assign update_buffer_addr_index      = update_buffer_addr[INDEX_WIDTH + OFFSET_WIDTH - 1 : OFFSET_WIDTH];
    assign update_buffer_addr_tag        = update_buffer_addr[DATA_ADDR_BUS - 1 : INDEX_WIDTH + OFFSET_WIDTH];

    //update数据连线
    assign update_tagv = {update_buffer_addr_tag, 1'b1};
    assign update_data = {ram_rdata, update_buffer_data[DATA_BLOCK_WIDTH - DATA_BUS - 1 : 0]};
    assign update_index = update_buffer_addr_index;

    //更新state
    always @(posedge clk) begin
        if (rst == `RST_ENABLE) begin
            cache_state <= STATE_FREE;
        end
        else 
        case (cache_state)
            STATE_FREE : begin
                cache_state <= cpu_req ? STATE_LOOK_UP : STATE_FREE;
            end
            STATE_LOOK_UP : begin
                cache_state <= request_buffer_uncached & !hit ? STATE_UPDATE : 
                                !hit ? STATE_RAM_REQ :
                                cpu_req ? STATE_LOOK_UP : STATE_FREE;
            end
            STATE_RAM_REQ : begin
                cache_state <= update_cnt == NUM_INST_PER_LINE - 2 ? STATE_UPDATE : STATE_RAM_REQ; //等待更新最后一个指令时，进入update状态
            end
            STATE_UPDATE : begin
                cache_state <= update_buffer_valid == {NUM_INST_PER_LINE{1'b1}} || (ram_data_ok && update_buffer_uncached)? STATE_LOOK_UP : STATE_UPDATE;
            end
        endcase
    end

    wire debug_update;
    wire[INDEX_WIDTH - 1 : 0] debug_exchange;
    assign debug_update = cache_state == STATE_UPDATE && !update_buffer_uncached && ram_data_ok ? 1 : 0;
    assign debug_exchange = debug_update ? update_index : 0;

    //更新update_buffer
    always @(posedge clk) begin
        if (rst == `RST_ENABLE) begin
            update_buffer_valid <= 0;
            update_buffer_uncached <= 0;
            update_cnt <= 0;
        end
        else if (cache_state == STATE_LOOK_UP && !hit) begin
            update_buffer_addr <= request_buffer_addr;
            update_buffer_valid <= 0;
            update_buffer_way <= free_way[0] ? free_way[ASSOC_WIDTH:1] : lfsr[ASSOC_WIDTH - 1 : 0];
            update_buffer_uncached <= request_buffer_uncached;
            update_cnt <= 0;
        end
        else if (cache_state == STATE_RAM_REQ || cache_state == STATE_UPDATE) begin
            if (ram_beat_ok) begin
                update_buffer_valid[update_cnt] <= 1;
                update_buffer_data[(update_cnt + 1) * DATA_BUS - 1 -: DATA_BUS] <= ram_rdata;
                update_cnt <= update_cnt + 1;
            end
        end
    end

    //更新ram_req     
    always @(posedge clk) begin
        if (rst == `RST_ENABLE || ram_data_ok) begin
            ram_req_flag <= 0;
        end
        else if (cache_state == STATE_LOOK_UP && !hit) begin
            ram_req_flag <= 1;
        end
    end  

    assign ram_req = cache_state == STATE_LOOK_UP && !hit || ram_req_flag ? {4{~ram_data_ok}} : 0;

    assign ram_addr = request_buffer_uncached ? request_buffer_addr : {request_buffer_addr[DATA_ADDR_BUS - 1 : OFFSET_WIDTH], {OFFSET_WIDTH{1'b0}}};
    // assign cpu_operation_ok = hit;
    assign cpu_operation_ok = allow_new_req;
                    
    assign ram_uncached = request_buffer_uncached;

	
endmodule