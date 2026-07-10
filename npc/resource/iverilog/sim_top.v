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
    ysyx_26010036 u_core(
        .clock(clock),
        .reset(reset)
    );

    // 4. 控制仿真时长与波形生成
    initial begin
        // 按照要求生成 FST 格式波形
        $dumpfile("wave.fst");
        // 记录 sim_top 及其所有子模块的波形
        $dumpvars(0, sim_top);

        // 设置安全超时时间，防止程序跑飞导致死循环，把硬盘写满
        #100000;
        $display("--- Simulation Timeout! ---");
        $finish;
    end
endmodule