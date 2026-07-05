#include <cstdio>
#include <cstdint>
#include <string>
#include "include/common.h"


static std::string branchtrace_path() {
    if (!build_dir.empty() && build_dir.back() == '/')
        return build_dir + "branchtrace.brt.bz2";
    return build_dir + "/branchtrace.brt.bz2";
}

static std::string branchtrace_read_path() {
    if (!build_dir.empty() && build_dir.back() == '/')
        return build_dir + "../resource/branchtrace.brt.bz2";
    return build_dir + "/../resource/branchtrace.brt.bz2";
}

/* ============================ 记录 (Write) ============================ */
namespace {
    FILE     *w_fp = nullptr;
    uint8_t   w_buf[1 << 16];
    size_t    w_buf_len = 0;

    // 延迟结算状态机
    bool      w_pending_branch = false;
    uint32_t  w_pending_pc = 0;
    bool      w_pending_is_bw = false;

    void w_flush_buf() {
        if (w_buf_len) { fwrite(w_buf, 1, w_buf_len, w_fp); w_buf_len = 0; }
    }
    
    inline void w_byte(uint8_t b) {
        if (w_buf_len == sizeof(w_buf)) w_flush_buf();
        w_buf[w_buf_len++] = b;
    }
} // namespace

bool branchtrace_write_init() {
    std::string cmd = "bzip2 -c > '" + branchtrace_path() + "'";
    w_fp = popen(cmd.c_str(), "w");
    w_buf_len = 0;
    w_pending_branch = false;
    
    if (w_fp) {
        // 写入 Magic Number: "BRT1" (Branch Trace v1)
        w_byte('B'); w_byte('R'); w_byte('T'); w_byte('1');
    }
    return w_fp != nullptr;
}

void branchtrace_write_record(uint32_t pc, uint32_t inst) {
    if (!w_fp) return;

    // 1. 结算上一次遇到的分支指令
    if (w_pending_branch) {
        // 假设是基础 32 位指令集(无C扩展), 如果新PC不等于 PC+4，说明发生了跳转
        bool is_taken = (pc != w_pending_pc + 4);
        
        // 编码打包: bit[1] 为方向 (1=向后, 0=向前), bit[0] 为是否跳转 (1=Taken, 0=Not Taken)
        uint8_t record = (w_pending_is_bw ? 2 : 0) | (is_taken ? 1 : 0);
        w_byte(record);
        
        w_pending_branch = false;
    }

    // 2. 检测当前指令是否为分支指令
    // RISC-V B-type 指令的 opcode 是 7'b1100011 (0x63)
    if ((inst & 0x7F) == 0x63) {
        w_pending_branch = true;
        w_pending_pc = pc;
        // inst[31] 为符号位: 1 代表偏移量为负 (向后跳转), 0 代表正 (向前跳转)
        w_pending_is_bw = ((inst >> 31) & 1) != 0; 
    }
}

bool branchtrace_write_close() {
    if (!w_fp) return false;
    // 丢弃最后一个未结算的分支 (因为程序结束了，无从知晓是否Taken)
    w_flush_buf();
    int rc = pclose(w_fp);
    w_fp = nullptr;
    return rc == 0;
}

/* ============================ 回放 (Read) ============================ */
namespace {
    FILE     *r_fp = nullptr;
    uint8_t   r_buf[1 << 16];
    size_t    r_buf_len = 0, r_buf_pos = 0;
    bool      r_eof = false;
    bool      r_header_done = false;

    int r_byte() {
        if (r_buf_pos == r_buf_len) {
            if (r_eof) return -1;
            size_t got = fread(r_buf, 1, sizeof(r_buf), r_fp);
            if (got == 0) { r_eof = true; return -1; }
            r_buf_len = got; r_buf_pos = 0;
        }
        return r_buf[r_buf_pos++];
    }
} // namespace

bool branchtrace_read_init() {
    std::string cmd = "bzcat '" + branchtrace_read_path() + "'";
    r_fp = popen(cmd.c_str(), "r");
    r_buf_len = r_buf_pos = 0;
    r_eof = false;
    r_header_done = false;
    return r_fp != nullptr;
}

// 每次调用，读出一条分支指令的信息
bool branchtrace_read_next(bool *is_backward, bool *is_taken) {
    if (!r_fp) return false;

    // 检查并跳过 Magic Number
    if (!r_header_done) {
        int m0 = r_byte(), m1 = r_byte(), m2 = r_byte(), m3 = r_byte();
        if (m0 < 0) return false; 
        if (m0 != 'B' || m1 != 'R' || m2 != 'T' || m3 != '1') return false; // 格式不符
        r_header_done = true;
    }

    int b = r_byte();
    if (b < 0) return false; // EOF: 文件结束

    // 解码
    *is_backward = (b & 2) != 0;
    *is_taken    = (b & 1) != 0;
    
    return true;
}

bool branchtrace_read_close() {
    if (!r_fp) return false;
    int rc = pclose(r_fp);
    r_fp = nullptr;
    return rc == 0;
}