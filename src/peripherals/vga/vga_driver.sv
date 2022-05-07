module vga_driver(
    input               clk,
    input               rst,
    // 控制信息
    input [31: 0]       ctrl,
    // 显示位置信息
    input [31: 0]       img_point,
    // 图像尺寸信息
    input [31: 0]       img_size,

    // 行同步信号
    output logic        hs,
    // 场同步信号
    output logic        vs,
    // 屏幕有效显示区标志
    output logic        vidon,
    // 图像有效显示器标志
    output logic        spirtron,
    // x 坐标
    output logic [9: 0] lcd_xpos,
    // y 坐标
    output logic [9: 0] lcd_ypos
);


    // 行时序参数
    localparam H_FRONT = 16'd16;
    localparam H_SYNC = 16'd96;
    localparam H_BACK = 16'd48;
    localparam H_DISP = 16'd640;
    localparam H_TOTAL = 16'd800;

    // 列时序参数
    localparam V_FORNT = 16'd10;
    localparam V_SYNC = 16'd2;
    localparam V_BACK = 16'd33;
    localparam V_DISP = 16'd480;
    localparam V_TOTAL = 16'd525;

    reg             vsenable; // 行计数达到最大值的标志位
    reg [9: 0]      hc, vc; // 行计数器和列计数器
    reg             start_flag; // VGA 控制器启动标志位

    wire [15: 0]    x, y; // 像素的坐标
    wire [15: 0]    width, height; // 图片宽高
    
    assign {x, y} = img_point;
    assign {height, width} = img_size;

    always@(posedge clk) begin 
        if(!rst) start_flag <= 1'b0;
        else if(ctrl > 0) start_flag <= 1'b1;
        else start_flag <= 1'b0;
    end

    // 用于控制状态
    always@(posedge clk)begin 
        if(~rst)begin 
            hc <= 0;
            vsenable <= 0;
        end else if(start_flag) begin 
            if(hc < H_TOTAL[9: 0] - 1)begin 
                hc <= hc + 1;
                vsenable <= 0;
            end else begin 
                hc <= 0;
                vsenable <= 1;
            end
        end else begin 
            hc <= 0; 
            vsenable <= 0;
        end
    end

    // VGA 控制器扫描的场计数，最大值为 525
    always@(posedge clk)begin 
        if(~rst)begin 
            vc <= 0;
        end else if(start_flag)begin 
            if(vsenable)begin 
                if(vc < V_TOTAL[9: 0] - 1)begin 
                    vc <= vc + 1;
                end else begin 
                    vc <= 0;
                end
            end
        end else begin 
            vc <= 0;
        end
    end

    assign hs = (rst && hc == H_SYNC[9: 0])? 1'b1: 1'b0;
    assign vs = (rst && vc == H_SYNC[9: 0])? 1'b1: 1'b0;

    assign vidon = ((hc < H_SYNC + H_BACK + H_DISP) && (hc >= H_SYNC + H_BACK) &&
            (vc < V_SYNC + V_BACK + V_DISP) && (vc >= V_SYNC + V_BACK))? 1'b1: 1'b0;

    assign spirtron = ((hc < H_SYNC + H_BACK + x + width) && (hc >= H_SYNC + H_BACK + x) &&
                        (vc < V_SYNC + V_BACK + y + height) && (vc >= V_SYNC + V_BACK + y))? 1'b1: 1'b0;

    assign lcd_xpos = (vidon)? {H_SYNC + H_BACK}: 0;
    assign lcd_ypos = (vidon)? (V_SYNC + V_BACK): 0;

endmodule