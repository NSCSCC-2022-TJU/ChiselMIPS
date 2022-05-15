/**********************************************/
//支持二值图像显示,显示分辨率640*480@60Hz     //
/**********************************************/
module vga_top(
   input 					clk, 				
   input 					rstn,              
   input	[31 : 0]		ctrl,		// 连接AXI4接口中的0号寄存器——slv_reg0
   input 	[31 : 0] 		impoint,	// 连接AXI4接口中的1号寄存器——slv_reg1
   input 	[31 : 0] 		imsize,		// 连接AXI4接口中的2号寄存器——slv_reg2
   input 	[31 : 0] 		bgcolor,	// 连接AXI4接口中的3号寄存器——slv_reg3
   input 	[0  :31] 		vmdata,

   output 					vmena,
   output 	[31 : 0] 		vmaddr,
   output  	[3  : 0] 		r,  
   output  	[3  : 0] 		g,  
   output  	[3  : 0] 		b,  
   output 					hs,  
   output 					vs
);
   wire vidon, spriteon;
   wire [9 : 0] lcd_xpos, lcd_ypos;
   
   vga_driver 	vga_driver( 	.clk(clk), .rstn(rstn), 
						.ctrl(ctrl), .impoint(impoint), .imsize(imsize), 
						.hs(hs), .vs(vs), .vidon(vidon), .spriteon(spriteon), 
						.lcd_xpos(lcd_xpos), .lcd_ypos(lcd_ypos));
					
					
					
   vga_data 	vga_data( 	.lcd_xpos(lcd_xpos), .lcd_ypos(lcd_ypos),.vidon(vidon), .spriteon(spriteon), 
						.bgcolor(bgcolor), .vmdata(vmdata), .vmaddr(vmaddr), .vmena(vmena), 
						.r(r), .g(g), .b(b) );

endmodule