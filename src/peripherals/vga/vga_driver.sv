module vga_driver(
	input 					clk,
	input 					rstn,
	input     	[31:0]      ctrl,  
	input		[31:0]      impoint,
	input   	[31:0]		imsize,
	
	output reg				hs, 
	output reg				vs,
	output reg				vidon,
	output reg				spriteon,
	output reg [9 : 0] 		lcd_xpos, 
	output reg [9 : 0]		lcd_ypos
);
	
	// 行时序参数
    localparam H_FRONT       = 16'd16;
    localparam H_SYNC        = 16'd96;
    localparam H_BACK        = 16'd48;
    localparam H_DISP        = 16'd640;
    localparam H_TOTAL       = 16'd800;
                               
    //列时序参数          
    localparam V_FRONT       = 16'd10;
    localparam V_SYNC        = 16'd2;
    localparam V_BACK        = 16'd33;
    localparam V_DISP        = 16'd480;
    localparam V_TOTAL       = 16'd525;
	
	reg 		   		vsenable;
	reg    	[9 : 0]		hc, vc;
	reg 				start_flag;
	
	wire	[15 : 0] 	X_POINT,	Y_POINT;
	wire 	[15 : 0] 	WIDTH, 		HEIGHT;
	
	assign {Y_POINT, X_POINT} 	= impoint;
	assign {HEIGHT, WIDTH} 		= imsize;
	
	always @(posedge clk) begin
	   if(!rstn) start_flag <= 'b0;
       else if (ctrl) start_flag <= 'b1;
       else start_flag <= 'b0;
	end
	
	// 行计数到800
	always @(posedge clk) begin
            
		if(!rstn) begin
            hc <= 0;
            vsenable <= 0;
        end
        else if (start_flag == 'b1) begin
                    
			if(hc == H_TOTAL - 1) begin
                            
				hc <= 0;
                vsenable <= 1;
                            
            end
            else begin
                            
				hc <= hc + 1; 
                vsenable <= 0;
            end
        end
        else begin
            hc <= 0;
            vsenable <= 0;
        end
            
    end  
      
    // 场计数到525
    always @(posedge clk) begin
              
		if(!rstn)
			vc <= 0;
        else if (start_flag == 'b1) begin
			if(vsenable == 1) begin
                              
				if(vc == V_TOTAL - 1)
					vc <= 0;
                else
					vc <= vc + 1;   // vc is used to record how many rows has been scanned in the screen                        
            end
                      
        end
        else vc <= 0;
              
    end 
      
    // 生成行同步与场同信号
    always @(*) begin
              
		if(hc < H_SYNC) hs = 0;
        else hs = 1;
              
    end
    always @(*) begin
                      
		if(vc < V_SYNC) vs = 0;
        else vs = 1;
                      
    end
	
	// 生成640×480屏幕有效显示区标志位
    always @(*) begin
              
		if((hc < H_SYNC + H_BACK + H_DISP) && (hc >= H_SYNC + H_BACK) && (vc < V_SYNC + V_BACK + V_DISP) && (vc >= V_SYNC + V_BACK))
            vidon = 1;
        else
            vidon = 0;              
    end
    
	// 生成图像有效显示区标志位
    always @(*) begin
                  
		if((hc < H_SYNC + H_BACK + X_POINT + WIDTH) && (hc >= H_SYNC + H_BACK + X_POINT) && (vc < V_SYNC + V_BACK + Y_POINT +  HEIGHT) && (vc >= V_SYNC + V_BACK + Y_POINT))
			spriteon = 1;
        else
			spriteon = 0;              
    end
	
	// 计算当前扫描像素点在屏幕中的坐标
	always @(*) begin
        if(vidon == 'b1) begin
               lcd_xpos = hc - (H_SYNC + H_BACK);
               lcd_ypos = vc - (V_SYNC + V_BACK);
        end
        else begin
               lcd_xpos = 0;
               lcd_ypos = 0;
        end
        
    end
endmodule