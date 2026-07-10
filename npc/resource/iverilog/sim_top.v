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

    // 3. 实例化 NPC 核心
    // 模块名必须与你 Makefile 中的 CORENAME 保持一致 (ysyx_26010036)
    ysyxSoCFull u_core(
        .clock(clock),
        .reset(reset)
    );

    // 4. 控制仿真时长与波形生成
    initial begin
        // 按照要求生成 FST 格式波形
        $dumpfile("wave.fst");
        // 记录 sim_top 及其所有子模块的波形
        $dumpvars(0, sim_top);

        // 运行 100000ns 后停止记录波形，防止硬盘写满
        #10000;
        $display("--- Waveform dumping stopped at %0t! Simulation continues... ---", $time);
        $dumpoff; // 核心修改：停止波形记录，但不结束仿真

        #50000; 
        $display("--- Absolute Simulation Timeout! ---");
        $finish;  // 彻底结束仿真
    end

    always @(posedge clock) begin
        // 1. 监控是否执行了 ebreak 并且已经提交
        // 这里的 u_core.xxx.ebreak_commit 只是一个例子，你需要替换成真实的层次路径
        if (u_core.u_exu.ebreak_commit_flag == 1'b1) begin 
            
            // 2. ebreak 触发时，读取内部寄存器堆的 a0 (x10) 寄存器
            // 按照 AM 的规约，a0 为 0 代表 GOOD TRAP，非 0 代表 BAD TRAP
            // 这里同样需要替换为真实的 x10 寄存器路径
            if (u_core.u_rf.rf_regs[10] == 32'd0) begin
                $display("\n=========================================");
                $display("       HIT GOOD TRAP (iverilog)");
                $display("=========================================\n");
            end else begin
                $display("\n=========================================");
                $display("       HIT BAD TRAP (iverilog)");
                $display("=========================================\n");
            end
            
            // 3. 结束仿真
            $finish;
        end
    end
endmodule