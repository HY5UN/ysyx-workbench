`timescale 1ns/1ps

module sim_top;
    reg clock;
    reg reset;

    // 1. 生成时钟：频率100MHz (周期10ns)
    initial begin
        clock = 0;
        forever #5 clock = ~clock;
    end

    // 2. 复位逻辑：初始拉高，保持 20ns 后释放
    initial begin
        reset = 1;
        #20;
        reset = 0;
    end

    ysyxSoCFull u_core(
        .clock(clock),
        .reset(reset)
    );

    // 4. 控制仿真时长与波形生成
    initial begin
        $dumpfile("wave.fst");
        // 记录 sim_top 及其所有子模块的波形
        $dumpvars(0, sim_top);

        // 运行 100000ns 后停止记录波形，防止硬盘写满
        #1000000;
        $display("--- Waveform dumping stopped at %0t! Simulation continues... ---", $time);
        $dumpoff; // 核心修改：停止波形记录，但不结束仿真

        #500000000; 
        $display("--- Absolute Simulation Timeout! ---");
        $finish;  // 彻底结束仿真
    end

    // always @(posedge clock) begin
    //     if (u_core.core.wbu.io_in_bits_ctrl_excType == 4'h3 && u_core.core.wbu.io_in_valid) begin     
    //         if (u_core.core.gpr.regFile_10 == 32'd0) begin
    //             $display("\n=========================================");
    //             $display("       HIT GOOD TRAP (iverilog)");
    //             $display("=========================================\n");
    //         end else begin
    //             $display("\n=========================================");
    //             $display("       HIT BAD TRAP (iverilog)");
    //             $display("=========================================\n");
    //         end
            
    //         $finish;
    //     end
    //     if (^u_core.core.ifu.io_out_bits_pc === 1'bx && $time > 100000) begin
    //         $display("\n=========================================");
    //         $display("FATAL: PC went to 'x' at time %0t!", $time);
    //         $display("=========================================\n");
    //         $finish; 
    //     end
    // end

    always @(posedge clock) begin
        if(^u_core.core.io_master_araddr=== 1'bx && $time > 100000)begin
            $display("\n=========================================");
            $display("FATAL: araddr went to 'x' at time %0t!", $time);
            $display("=========================================\n");
            $finish; 
        end
        if(^u_core.core.io_master_rdata=== 1'bx && $time > 100000)begin
            $display("\n=========================================");
            $display("FATAL: araddr went to 'x' at time %0t!", $time);
            $display("=========================================\n");
            $finish; 
        end
    end
endmodule