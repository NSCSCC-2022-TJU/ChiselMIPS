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

module data_cache #(parameter
	TAG_WIDTH = `TAG_WIDTH,
	INDEX_WIDTH = `INDEX_WIDTH,
	OFFSET_WIDTH = `OFFSET_WIDTH,
	NUM_ASSOC = `DATA_NUM_ASSOC,
    DATA_ADDR_BUS = 32,
    DATA_BUS = 32
) (
	// clock and reset
	input  wire 					                                clk,
	input  wire 					                                rst,

	//与MiniMIPS32处理器核互连的信号                                
    input  wire                 	                                cpu_req,
    input  wire [3   :   0  	]	                                cpu_wre, //读为0
    input  wire                                                     cpu_wr, //读为0
    input  wire [`DATA_ADDR_BUS	]                                   cpu_addr,
    input  wire [`DATA_BUS		]                                   cpu_wdata,
    input  wire                                                     cpu_uncached,

	output wire                                                     cpu_addr_ok,
    output wire                                                     cpu_operation_ok,
    output reg [`DATA_BUS		]                                   cpu_rdata,

	//类sram接口                                
    output wire            	                                        ram_rreq,
    output wire            	                                        ram_wreq,
    output wire [3   :   0  	]	                                ram_we,
    output wire 					                                ram_runcached,
    output wire 					                                ram_wuncached,
    output wire [`DATA_ADDR_BUS	]                                   ram_raddr,
    output wire [`DATA_ADDR_BUS	]                                   ram_waddr,
    output wire [DATA_BUS * (2 ** (OFFSET_WIDTH - 2)) - 1 : 0]      ram_wdata,

    input  wire 					                                ram_waddr_ok,
    input  wire 					                                ram_raddr_ok,
	input  wire						                                ram_beat_ok,
    input  wire 					                                ram_data_ok,
    input  wire [`DATA_BUS		]                                   ram_rdata
);
    parameter NUM_CACHE_LINES       = (2 ** INDEX_WIDTH); //Cache行数
    parameter NUM_INST_PER_LINE     = (2 ** (OFFSET_WIDTH - 2)); //每行的指令数目
    parameter DATA_BLOCK_WIDTH      = DATA_BUS * NUM_INST_PER_LINE;
    parameter ASSOC_WIDTH           = $clog2(NUM_ASSOC);

	
    //CPU接口声明
    wire [INDEX_WIDTH - 1 : 0]                      cpu_addr_index;
    wire [OFFSET_WIDTH - 3 : 0]                     cpu_addr_offset;
    wire                                            cpu_addr_conflict;

    assign cpu_addr_index = cpu_addr[OFFSET_WIDTH + INDEX_WIDTH - 1: OFFSET_WIDTH];
    assign cpu_addr_offset = cpu_addr[OFFSET_WIDTH - 1 : 2];

    //RAM接口声明
    reg  [3 : 0]                                    data_ram_we     [NUM_ASSOC*NUM_INST_PER_LINE-1:0];
    reg  [INDEX_WIDTH-1:0]                          data_ram_addr   [NUM_ASSOC*NUM_INST_PER_LINE-1:0];
    wire [DATA_BUS-1:0]                             data_ram_dout   [NUM_ASSOC*NUM_INST_PER_LINE-1:0];
    reg  [DATA_BUS-1:0]                             data_ram_din    [NUM_INST_PER_LINE-1:0];

    reg  [NUM_ASSOC-1:0]                            tagv_ram_we;
    reg  [INDEX_WIDTH-1:0]                          tagv_ram_addr;
    wire [TAG_WIDTH:0]                              tagv_ram_dout   [NUM_ASSOC-1:0];
    wire [TAG_WIDTH:0]                              tagv_ram_din;

	//CACHE_STATE声明
    reg [2 : 0]                                     cache_state;

    parameter                                       STATE_FREE                      = 0;
    parameter                                       STATE_LOOK_UP                   = 1;
    parameter                                       STATE_RAM_REQ                   = 2;
    parameter                                       STATE_UPDATE                    = 3;
    parameter                                       STATE_UNCACHED_STALL            = 4;

    wire                                            allow_new_req;

    //REPLACE_STATE声明
    reg [1 : 0]                                     replace_state;

    parameter                                       REPLACE_STATE_FREE              = 0;
    parameter                                       REPLACE_STATE_LOOK_UP           = 1;
    parameter                                       REPLACE_STATE_LOOK_UP_COMPLETED = 2;
    parameter                                       REPLACE_STATE_WRITE             = 3;

    //REPLACE_BUFFER声明
    reg [DATA_BLOCK_WIDTH - 1 : 0]                  replace_buffer_data;
    reg [DATA_ADDR_BUS - 1 : 0]                     replace_buffer_addr;
    reg                                             replace_buffer_uncached;
    reg [ASSOC_WIDTH - 1 : 0]                       replace_buffer_way;
    reg [3 : 0]                                     replace_buffer_we;
 
    //REQUEST_BUFFER声明
        //REQUEST_BUFFER = {CPU_ADDR, CPU_REQ, CPU_UNCACHED}
    reg [DATA_ADDR_BUS - 1 : 0]                     request_buffer_addr; 
    reg                                             request_buffer_uncached;
    reg [DATA_BUS - 1 : 0]                          request_buffer_wdata;
    reg [3 : 0]                                     request_buffer_wre;
    reg                                             request_buffer_wr;

    wire [TAG_WIDTH - 1 : 0]                        request_buffer_addr_tag;
    wire [INDEX_WIDTH - 1 : 0]                      request_buffer_addr_index;
    wire [OFFSET_WIDTH - 3 : 0]                     request_buffer_addr_offset;
    wire [DATA_BUS - 1 : 0]                         requset_buffer_wre_mask;

    //UPDATE_BUFFER声明
        //UPDATE_BUFFER = {UPDATE_BUFFER_ADDR, UPDATE_BUFFER_VALID, UPDATE_BUFFER_DATA}

    reg [DATA_ADDR_BUS - 1 : 0]                     update_buffer_addr;
    reg [NUM_INST_PER_LINE * 4 - 1 : 0]             update_buffer_valid;
    reg [DATA_BLOCK_WIDTH - 1 : 0]                  update_buffer_data;
    reg [ASSOC_WIDTH - 1 : 0]                       update_buffer_way;
    reg                                             update_buffer_uncached;
    reg                                             update_buffer_dirty;
    
    wire [INDEX_WIDTH - 1 : 0]                      update_buffer_addr_index;
    wire [TAG_WIDTH - 1 : 0]                        update_buffer_addr_tag;
    wire [3 : 0]                                    update_buffer_wre_beat;
    wire [3 : 0]                                    update_buffer_wre_nobeat;
    wire [3 : 0]                                    update_buffer_wre_valid;
    wire [3 : 0]                                    update_buffer_wre_valid_update;
    wire [DATA_BUS - 1 : 0]                         update_buffer_wre_beat_mask;
    wire [DATA_BUS - 1 : 0]                         update_buffer_wre_nobeat_mask;
    wire [DATA_BUS - 1 : 0]                         update_buffer_valid_mask;
    wire [DATA_BUS - 1 : 0]                         update_buffer_wre_valid_update_mask;

    reg                                             hit_update_buffer;

    //lookup声明
    reg                                             hit;
    reg [ASSOC_WIDTH - 1 : 0]                       hit_way;
    reg [ASSOC_WIDTH : 0]                           free_way; //free_way = {free_index, valid}
    reg                                             ram_req_flag;//指示ram请求何时停止

    //update声明
    reg [(OFFSET_WIDTH - 2) : 0]                    update_cnt;

    wire [TAG_WIDTH : 0]                            update_tagv;
    wire [DATA_BLOCK_WIDTH - 1 : 0]                 update_data;
    wire [INDEX_WIDTH - 1 : 0]                      update_index;
    
    //replace声明
    wire                                            replace_req;

    //LFSR声明
    reg [7 : 0]                                     lfsr;           //位宽越大，越随机，低位宽，0的概率会较低

    always @(posedge clk) begin
        if (rst == `RST_ENABLE) lfsr <= 3'b111;
        else lfsr <= {lfsr[6], lfsr[5] ^ lfsr[7], lfsr[4] ^ lfsr[7], lfsr[3] ^ lfsr[7], lfsr[2], lfsr[1], lfsr[0], lfsr[7]};
    end

    integer i;
    integer j;

    //ram接口连线
    always @(*) begin
        tagv_ram_addr = request_buffer_addr_index;
        tagv_ram_we = 0;
        for (i=0; i<NUM_ASSOC; i=i+1) begin
            for (j=0; j<NUM_INST_PER_LINE; j=j+1) begin
                data_ram_addr[i * NUM_INST_PER_LINE + j] = request_buffer_addr_index;
                data_ram_we[i * NUM_INST_PER_LINE + j] = 0; 
                data_ram_din[j] = request_buffer_wdata;

                if (cache_state == STATE_UPDATE && ram_data_ok && !update_buffer_uncached) begin
                    data_ram_addr[i * NUM_INST_PER_LINE + j] = update_index;
                    tagv_ram_addr = update_index;
                    data_ram_din[j] = update_data[(j + 1) * DATA_BUS - 1 -: DATA_BUS];
                    
                    if (i == update_buffer_way) begin
                        data_ram_we[i * NUM_INST_PER_LINE + j] = 4'b1111;
                        tagv_ram_we[i] = 1;
                    end
                end
                else if (allow_new_req && cpu_req) begin
                    data_ram_addr[i * NUM_INST_PER_LINE + j] = cpu_addr_index;
                    tagv_ram_addr = cpu_addr_index;
                end
                else if (replace_state == REPLACE_STATE_LOOK_UP) begin
                    data_ram_addr[i * NUM_INST_PER_LINE + j] = request_buffer_addr_index;
                end
                //write hit
                if ((cache_state == STATE_LOOK_UP || cache_state == STATE_RAM_REQ && !hit_update_buffer) && hit && request_buffer_wr == 1 && request_buffer_addr_offset == j && hit_way == i) begin    
                    data_ram_addr[i * NUM_INST_PER_LINE + j] = request_buffer_addr_index;
                    data_ram_we[i * NUM_INST_PER_LINE + j] = request_buffer_wre;
                end
            end
        end
    end

    // assign data_ram_din     = cache_state == STATE_UPDATE ? update_data : request_buffer_wdata;
    assign tagv_ram_din     = update_tagv;	

    //生成d表
    reg [NUM_ASSOC - 1 : 0] dirty [NUM_CACHE_LINES - 1 : 0];

    //生成数据ram
    generate
        genvar m;
        genvar p;
        for (m=0; m<NUM_ASSOC; m=m+1) begin:DATA_RAM_1
            for (p=0; p<NUM_INST_PER_LINE; p=p+1) begin:DATA_RAM_2
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

                    .ADDR_WIDTH_A(INDEX_WIDTH),                         // 地址宽度
                    .BYTE_WRITE_WIDTH_A(8),                             // 字节宽度；若不启用写字节使能则与WRITE_DATA_WIDTH_A一致
                    .READ_DATA_WIDTH_A(DATA_BUS),                       // The values of READ_DATA_WIDTH_A and WRITE_DATA_WIDTH_A must be equal.
                    .WRITE_DATA_WIDTH_A(DATA_BUS),                      // The values of READ_DATA_WIDTH_A and WRITE_DATA_WIDTH_A must be equal.
                    .MEMORY_SIZE(NUM_CACHE_LINES * DATA_BUS)            // (2**addr_width)*data_width
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
                    .addra(data_ram_addr[p + m * NUM_INST_PER_LINE]),                   
                    .wea(data_ram_we[p + m * NUM_INST_PER_LINE]),                        // 当word-wide write时，为1bit，否则与字节宽度有关；例如32bits宽度需要4bits input
                    .douta(data_ram_dout[p + m * NUM_INST_PER_LINE]),                   
                    .dina(data_ram_din[p])                    
                );
            end
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
    assign requset_buffer_wre_mask      = {{8{request_buffer_wre[3]}},{8{request_buffer_wre[2]}},{8{request_buffer_wre[1]}},{8{request_buffer_wre[0]}}};

    //更新requst_buffer
    always @(posedge clk) begin
        if (rst == `RST_ENABLE) begin
            request_buffer_addr     <= 0;
            request_buffer_uncached <= 0;
            request_buffer_wre      <= 0;
            request_buffer_wr       <= 0;
        end
        else if (allow_new_req && cpu_req) begin
            request_buffer_addr     <= cpu_addr;  //解决取指地址非对齐异常
            request_buffer_uncached <= cpu_uncached;
            request_buffer_wre      <= cpu_wre;
            request_buffer_wr       <= cpu_wr;
            request_buffer_wdata    <= cpu_wdata;
        end
        else if (request_buffer_wr && hit) begin
            request_buffer_wr <= 0;
        end
    end

    //lookup连线
    assign cpu_addr_conflict = request_buffer_wr == 1 && cpu_wr == 0 && hit && request_buffer_addr_offset == cpu_addr_offset;

    assign allow_new_req = cache_state == STATE_FREE ? 1 :
                            replace_req && (replace_state == REPLACE_STATE_FREE && ram_waddr_ok) && request_buffer_uncached ? 1 :
                            cache_state == STATE_UNCACHED_STALL ? 0 :
                            cpu_addr_conflict || replace_state == REPLACE_STATE_LOOK_UP ? 0 :
                            cache_state != STATE_UPDATE ? hit :
                            update_buffer_uncached && ram_data_ok ? 1 : 0;
    
    assign cpu_addr_ok = allow_new_req;

    // hit判断
    always @(*) begin
        hit = 0;
        hit_way = 0;
        cpu_rdata = 0;
        free_way = 0;
        hit_update_buffer = 0;
        if (!request_buffer_uncached && cache_state != STATE_UPDATE) begin 
            for (i=0; i<NUM_ASSOC; i=i+1) begin
                if (tagv_ram_dout[i][0]) begin
                    if (tagv_ram_dout[i][TAG_WIDTH : 1] == request_buffer_addr_tag) begin
                        hit = 1;
                        hit_way = i;
                        cpu_rdata = data_ram_dout[i * NUM_INST_PER_LINE + request_buffer_addr_offset];
                    end
                end
                else begin
                    free_way = {i[ASSOC_WIDTH - 1 : 0], 1'd1};
                end
            end
            if (!update_buffer_uncached && cache_state == STATE_RAM_REQ) begin
                if (hit_way == update_buffer_way && request_buffer_addr_index == update_buffer_addr_index && request_buffer_wr == 1) begin  //dirty为0，且发生替换，不会产生replace
                    hit = 0;
                end
                if ({request_buffer_addr_tag, request_buffer_addr_index} == replace_buffer_addr[DATA_BUS - 1 : OFFSET_WIDTH] && request_buffer_wr == 1 && replace_state != REPLACE_STATE_FREE) begin
                    hit = 0;
                end
                else if ({request_buffer_addr_tag, request_buffer_addr_index} == {update_buffer_addr_tag, update_buffer_addr_index} && request_buffer_wr == 1) begin
                    hit = 1;
                    hit_update_buffer = 1;
                    hit_way = update_buffer_way;
                end
                else if ({request_buffer_addr_tag, request_buffer_addr_index} == {update_buffer_addr_tag, update_buffer_addr_index} && update_buffer_valid[(request_buffer_addr_offset + 1) * 4 - 1 -: 4] == request_buffer_wre) begin
                    hit = 1;
                    hit_update_buffer = 1;
                    hit_way = update_buffer_way;
                    cpu_rdata = update_buffer_data[(request_buffer_addr_offset + 1) * DATA_BUS - 1 -: DATA_BUS];
                end
                else if ({request_buffer_addr_tag, request_buffer_addr_index} == {update_buffer_addr_tag, update_buffer_addr_index} && update_cnt == request_buffer_addr_offset && ram_beat_ok) begin
                    hit = 1;
                    hit_update_buffer = 1;
                    hit_way = update_buffer_way;
                    cpu_rdata = ram_rdata;
                end
            end
        end
        else begin
            if (request_buffer_addr == update_buffer_addr && update_buffer_uncached && ram_data_ok) begin
                hit = 1;
                cpu_rdata = ram_rdata;
            end
        end
    end


    //update_buffer连线
    assign update_buffer_addr_index      = update_buffer_addr[INDEX_WIDTH + OFFSET_WIDTH - 1 : OFFSET_WIDTH];
    assign update_buffer_addr_tag        = update_buffer_addr[DATA_ADDR_BUS - 1 : INDEX_WIDTH + OFFSET_WIDTH];
    assign update_buffer_wre_beat   = (request_buffer_wre | update_buffer_valid[(update_cnt + 1) * 4 - 1 -: 4]) ^ request_buffer_wre;
    assign update_buffer_wre_nobeat = (request_buffer_wre | update_buffer_valid[(request_buffer_addr_offset + 1) * 4 - 1 -: 4]) ^ request_buffer_wre;
    assign update_buffer_wre_valid = update_buffer_valid[(update_cnt + 1) * 4 - 1 -: 4];
    assign update_buffer_wre_valid_update = update_buffer_valid[NUM_INST_PER_LINE * 4 - 1 -: 4];
    assign update_buffer_wre_beat_mask = {{8{update_buffer_wre_beat[3]}},{8{update_buffer_wre_beat[2]}},{8{update_buffer_wre_beat[1]}},{8{update_buffer_wre_beat[0]}}};
    assign update_buffer_wre_nobeat_mask = {{8{update_buffer_wre_nobeat[3]}},{8{update_buffer_wre_nobeat[2]}},{8{update_buffer_wre_nobeat[1]}},{8{update_buffer_wre_nobeat[0]}}};
    assign update_buffer_valid_mask = {{8{update_buffer_wre_valid[3]}},{8{update_buffer_wre_valid[2]}},{8{update_buffer_wre_valid[1]}},{8{update_buffer_wre_valid[0]}}};
    assign update_buffer_wre_valid_update_mask = {{8{update_buffer_wre_valid_update[3]}},{8{update_buffer_wre_valid_update[2]}},{8{update_buffer_wre_valid_update[1]}},{8{update_buffer_wre_valid_update[0]}}};
    //update数据连线
    assign update_tagv = {update_buffer_addr_tag, 1'b1};
    assign update_data = {(ram_rdata & ~update_buffer_wre_valid_update_mask) | (update_buffer_data[NUM_INST_PER_LINE * DATA_BUS - 1 -: DATA_BUS] & update_buffer_wre_valid_update_mask), 
                            update_buffer_data[DATA_BLOCK_WIDTH - DATA_BUS - 1 : 0]};

                            
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
                    cache_state <= request_buffer_uncached && !request_buffer_wr ? STATE_UPDATE : 
                                    replace_req && !(replace_state == REPLACE_STATE_FREE && ram_waddr_ok) ? STATE_UNCACHED_STALL :
                                    !hit && !request_buffer_uncached ? STATE_RAM_REQ :
                                    cpu_req ? STATE_LOOK_UP : STATE_FREE;
                end
                STATE_RAM_REQ : begin
                    cache_state <= update_cnt == NUM_INST_PER_LINE - 3 ? STATE_UPDATE : STATE_RAM_REQ; //等待更新最后两个指令时，进入update状态，防止update前一个周期有对update_buffer写最高位的操作，导致update失败
                end
                STATE_UPDATE : begin
                    cache_state <= update_cnt == NUM_INST_PER_LINE ? STATE_LOOK_UP : 
                                    !(ram_data_ok && update_buffer_uncached) ? STATE_UPDATE :
                                    cpu_req ? STATE_LOOK_UP : STATE_FREE;
                end
                STATE_UNCACHED_STALL : begin
                    cache_state <= !(replace_state == REPLACE_STATE_FREE && ram_waddr_ok) ? STATE_UNCACHED_STALL : STATE_LOOK_UP;
                end
            endcase
    end

    //更新replace_state
    assign replace_req = (cache_state == STATE_LOOK_UP && !hit && !(request_buffer_uncached && !request_buffer_wr) && 
                        ((!free_way[0] && dirty[request_buffer_addr_index][lfsr[ASSOC_WIDTH - 1 : 0]]) || (request_buffer_uncached && request_buffer_wr)));
    always @(posedge clk) begin
        if (rst == `RST_ENABLE) begin
            replace_state <= REPLACE_STATE_FREE;
        end
        else    
            case (replace_state) 
                REPLACE_STATE_FREE : begin
                    replace_state <= !(replace_req && ram_waddr_ok)? REPLACE_STATE_FREE :
                                        request_buffer_uncached ? REPLACE_STATE_WRITE : REPLACE_STATE_LOOK_UP;
                end
                REPLACE_STATE_LOOK_UP : begin
                    replace_state <= REPLACE_STATE_LOOK_UP_COMPLETED;
                end
                REPLACE_STATE_LOOK_UP_COMPLETED : begin
                    replace_state <= REPLACE_STATE_WRITE;
                end
                REPLACE_STATE_WRITE : begin
                    replace_state <= !ram_waddr_ok ? REPLACE_STATE_WRITE : REPLACE_STATE_FREE;
                end
            endcase
    end

    //更新replace_buffer
    always @(posedge clk) begin
        if (rst == `RST_ENABLE) begin
            replace_buffer_addr <= 0; //有用，勿删，初始时参与其他比较。
        end
        else if (replace_req && replace_state == REPLACE_STATE_FREE && ram_waddr_ok) begin
            replace_buffer_we <= request_buffer_wre;
            replace_buffer_way <= lfsr[ASSOC_WIDTH - 1 : 0];
            replace_buffer_addr <= request_buffer_uncached ? request_buffer_addr : {tagv_ram_dout[lfsr[ASSOC_WIDTH - 1 : 0]][TAG_WIDTH:1], request_buffer_addr_index, {OFFSET_WIDTH{1'b0}}};
            replace_buffer_uncached <= request_buffer_uncached;
            replace_buffer_data [DATA_BUS - 1 : 0] <= request_buffer_wdata;
        end
        else if (replace_state == REPLACE_STATE_LOOK_UP_COMPLETED) begin
            for (i=0; i<NUM_INST_PER_LINE; i=i+1)
                replace_buffer_data[(i + 1) * DATA_BUS - 1 -: DATA_BUS] <= data_ram_dout[i + replace_buffer_way * NUM_INST_PER_LINE];
        end
            
    end

    //更新d表
    always @(posedge clk) begin
        if (rst == `RST_ENABLE) begin
            for (i=0; i<NUM_CACHE_LINES; i=i+1) 
                dirty[i] <= 0;
        end
        else if (cache_state == STATE_UPDATE && !update_buffer_uncached) begin
            dirty[update_buffer_addr_index][update_buffer_way] <= update_buffer_dirty;
        end
        else if ((cache_state == STATE_LOOK_UP || cache_state == STATE_RAM_REQ) && request_buffer_wr == 1 && hit) begin
            dirty[request_buffer_addr_index][hit_way] <= 1;
        end
            
    end

    //debug 指示是否发生替换
    wire debug_update;
    wire debug_exchange;
    assign debug_update = cache_state == STATE_UPDATE && !update_buffer_uncached && ram_data_ok ? 1 : 0;
    assign debug_exchange = debug_update ? !free_way[0] : 0;

    //更新update_buffer
    always @(posedge clk) begin
        if (rst == `RST_ENABLE) begin
            update_buffer_valid     <= 0;
            update_buffer_uncached  <= 0;
            update_cnt              <= 0;
            update_buffer_dirty     <= 0;
        end
        else if (cache_state == STATE_LOOK_UP && !hit) begin
            update_buffer_addr      <= request_buffer_addr;
            update_buffer_way       <= free_way[0] ? free_way[ASSOC_WIDTH:1] : lfsr[ASSOC_WIDTH - 1 : 0];
            update_buffer_uncached  <= request_buffer_uncached;
            update_cnt              <= 0;
            // read
            if (request_buffer_wr == 0) begin
                update_buffer_valid <= 0;
                update_buffer_dirty <= 0;
            end
            // write
            else if (!request_buffer_uncached) begin
                update_buffer_data[(request_buffer_addr_offset + 1) * DATA_BUS - 1 -: DATA_BUS] <= request_buffer_wdata;
                update_buffer_dirty <= 1;
                for (i=0; i<NUM_INST_PER_LINE; i=i+1) begin
                    if (i == request_buffer_addr_offset)
                        update_buffer_valid[(i + 1) * 4 - 1 -: 4] <= request_buffer_wre;
                    else
                        update_buffer_valid[(i + 1) * 4 - 1 -: 4] <= 0;
                end
            end
        end
        else if (cache_state == STATE_RAM_REQ || cache_state == STATE_UPDATE) begin
            if (ram_beat_ok) begin
                update_buffer_valid[(update_cnt + 1) * 4 - 1 -: 4] <= 4'b1111;
                update_cnt <= update_cnt + 1;
                if (request_buffer_wr == 1 && hit_update_buffer) begin //优化时序，可去掉beat_ok时的写名中，触发概率较低
                    update_buffer_dirty <= 1;
                    if (request_buffer_addr_offset == update_cnt) begin
                        update_buffer_data[(update_cnt + 1) * DATA_BUS - 1 -: DATA_BUS] <= (request_buffer_wdata & requset_buffer_wre_mask) | 
                                                                                        ((update_buffer_wre_beat_mask) & update_buffer_data[(update_cnt + 1) * DATA_BUS - 1 -: DATA_BUS]) |
                                                                                        (~(update_buffer_wre_beat_mask) & ram_rdata);
                    end
                    else begin
                        update_buffer_data[(update_cnt + 1) * DATA_BUS - 1 -: DATA_BUS] <= (ram_rdata & ~update_buffer_valid_mask) | 
                                                                                            (update_buffer_data[(update_cnt + 1) * DATA_BUS - 1 -: DATA_BUS] & update_buffer_valid_mask);
                        update_buffer_valid[(request_buffer_addr_offset + 1) * 4 - 1 -: 4] <= request_buffer_wre | update_buffer_valid[(request_buffer_addr_offset + 1) * 4 - 1 -: 4];
                        update_buffer_data[(request_buffer_addr_offset + 1) * DATA_BUS - 1 -: DATA_BUS] <= (request_buffer_wdata & requset_buffer_wre_mask) | 
                                                                                                            ((update_buffer_wre_nobeat_mask) & update_buffer_data[(request_buffer_addr_offset + 1) * DATA_BUS - 1 -: DATA_BUS]);
                    end
                end
                else begin
                    update_buffer_data[(update_cnt + 1) * DATA_BUS - 1 -: DATA_BUS] <= (ram_rdata & ~update_buffer_valid_mask) | 
                                                                                        (update_buffer_data[(update_cnt + 1) * DATA_BUS - 1 -: DATA_BUS] & update_buffer_valid_mask);
                end
            end
            else if (request_buffer_wr == 1 && hit_update_buffer) begin
                update_buffer_dirty <= 1;
                update_buffer_valid[(request_buffer_addr_offset + 1) * 4 - 1 -: 4] <= request_buffer_wre | update_buffer_valid[(request_buffer_addr_offset + 1) * 4 - 1 -: 4];
                update_buffer_data[(request_buffer_addr_offset + 1) * DATA_BUS - 1 -: DATA_BUS] <= (request_buffer_wdata & requset_buffer_wre_mask) | 
                                                                                    ((update_buffer_wre_nobeat_mask) & update_buffer_data[(request_buffer_addr_offset + 1) * DATA_BUS - 1 -: DATA_BUS]);
            end
            if (update_cnt == NUM_INST_PER_LINE) begin
                update_buffer_valid <= 0;
            end
        end
    end

    //更新ram_req
    always @(posedge clk) begin
        if (rst == `RST_ENABLE || ram_raddr_ok) begin
            ram_req_flag <= 0;
        end
        else if (cache_state == STATE_LOOK_UP && !hit && !(request_buffer_uncached && request_buffer_wr)) begin
            ram_req_flag <= 1;
        end
    end  

    assign ram_rreq = (replace_req && !(replace_state == REPLACE_STATE_FREE && ram_waddr_ok)) || cache_state == STATE_UNCACHED_STALL ? 0 :
                        request_buffer_uncached && request_buffer_wr && cache_state == STATE_LOOK_UP ? 0 :
                        cache_state == STATE_LOOK_UP && !hit || ram_req_flag ? ~ram_data_ok : 0;
    
    assign ram_wreq = replace_state == REPLACE_STATE_WRITE;

    assign ram_raddr = cache_state == STATE_RAM_REQ ? {update_buffer_addr[DATA_ADDR_BUS - 1 : OFFSET_WIDTH], {OFFSET_WIDTH{1'b0}}} :
                        cache_state == STATE_UPDATE ? update_buffer_addr :
                        request_buffer_uncached ? request_buffer_addr : {request_buffer_addr[DATA_ADDR_BUS - 1 : OFFSET_WIDTH], {OFFSET_WIDTH{1'b0}}};

    assign ram_waddr = replace_buffer_addr;
    assign ram_we = replace_buffer_we;

    assign cpu_operation_ok = allow_new_req;
                    
    assign ram_runcached = request_buffer_uncached;
    assign ram_wuncached = replace_buffer_uncached;

    assign ram_wdata = replace_buffer_data;


endmodule