module VRandomDelay #(
    parameter DELAY_BITS = 4    // 最大延迟 = 2^DELAY_BITS - 1 周期
)(
    input  wire clock,
    input  wire reset,
    input  wire trigger,    // 触发一次随机延迟
    output reg  ready       // 延迟结束信号
);

    // ── 8位 LFSR ────────────────────────────────────────────────
    reg [7:0] lfsr;
    always @(posedge clock) begin
        if (reset)
            lfsr <= 8'hAC;  // 非零种子
        else if (trigger)
            lfsr <= {lfsr[6:0], lfsr[7] ^ lfsr[5] ^ lfsr[4] ^ lfsr[3]};
    end

    // ── 倒计时计数器 ─────────────────────────────────────────────
    reg [DELAY_BITS-1:0] cnt;
    reg active;

    always @(posedge clock) begin
        if (reset) begin
            cnt    <= 0;
            active <= 0;
            ready  <= 1;
        end else begin
            if (trigger && !active) begin
                cnt    <= lfsr[DELAY_BITS-1:0];  // 取 LFSR 低几位作延迟值
                active <= 1;
                ready  <= 0;
            end else if (active) begin
                if (cnt == 0) begin
                    active <= 0;
                    ready  <= 1;
                end else begin
                    cnt <= cnt - 1;
                end
            end
        end
    end

endmodule