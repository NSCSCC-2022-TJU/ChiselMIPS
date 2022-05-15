module vga_data( 
	input 	[9 :  0] 	lcd_xpos, 
	input 	[9 :  0]	lcd_ypos,
	input 				vidon,
	input 				spriteon,
	input 	[31 : 0]    bgcolor,
	input 	[0 : 31] 	vmdata,
	
	output 	[3 :  0] 	r, 
	output 	[3 :  0] 	g, 
	output 	[3 :  0] 	b, 
	output              vmena,
	output 	[31 : 0]	vmaddr
);

	wire [12 : 0] BGC;		// 背景颜色编码
	wire [0 : 31] vmdata;   
    wire [31 : 0] vmaddr;
	
	reg [13 : 0] row_addr;	//显存存储单元地址
    reg [ 4 : 0] col_addr;
	reg [11 : 0] color;
	
	assign BGC = bgcolor;
    
	// 计算当前扫描点在显存中的位置
    always @(*) begin
        row_addr = (lcd_ypos * 20) + ((lcd_xpos) >> 5);
        col_addr = (lcd_xpos) - (((lcd_xpos) >>5)<<5);
    end
    
    assign vmaddr = {16'b0000_0000_0000_0000,row_addr, 2'b00};	
    assign vmena = 1'b1;
    
	// 生成扫描像素点的颜色
    always @(*) begin
        if(vidon == 'b1) begin
            if(spriteon == 'b1) begin
                color = (vmdata[col_addr] == 1) ? 12'b0000_0000_0000 : 12'b1111_1111_1111;
            end
            else
                color = BGC;
        end
        else color = 12'b0000_0000_0000;
    end
                     
	assign r = { color[11],color[10],color[9],color[8] };
	assign g = { color[7 ],color[6 ],color[5],color[4] };
	assign b = { color[3 ],color[2 ],color[1],color[0] };
	
endmodule
