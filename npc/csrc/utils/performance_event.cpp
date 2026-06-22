#include "include/common.h"
#include <svdpi.h>

long long cnt_instfetch = 0;
long long cnt_lsu_r = 0;
long long cnt_lsu_w = 0;
long long cnt_exu = 0;
long long cnt_inst_r = 0;
long long cnt_inst_i = 0;
long long cnt_inst_l = 0;
long long cnt_inst_s = 0;
long long cnt_inst_u = 0;
long long cnt_inst_b = 0;
long long cnt_inst_j = 0;
long long cnt_inst_csr = 0;
long long cnt_inst_sys = 0;


extern "C" void dpic_save_performance_event(
    svBit io_instfetch,
    svBit io_lsu_r,
    svBit io_lsu_w,
    svBit io_exu,
    svBit io_inst_r,
    svBit io_inst_i,
    svBit io_inst_l,
    svBit io_inst_s,
    svBit io_inst_u,
    svBit io_inst_b,
    svBit io_inst_j,
    svBit io_inst_csr,
    svBit io_inst_sys
) {
    cnt_instfetch += io_instfetch;
    cnt_lsu_r += io_lsu_r;
    cnt_lsu_w += io_lsu_w;
    cnt_exu += io_exu;
    cnt_inst_r += io_inst_r;
    cnt_inst_i += io_inst_i;
    cnt_inst_l += io_inst_l;
    cnt_inst_s += io_inst_s;
    cnt_inst_u += io_inst_u;
    cnt_inst_b += io_inst_b;
    cnt_inst_j += io_inst_j;
    cnt_inst_csr += io_inst_csr;
    cnt_inst_sys += io_inst_sys;
}

void print_performance_counters() {
    printf("Performance Counters:\n");
    printf("  Instruction Fetch: %lld\n", cnt_instfetch);
    printf("  Load/Store Unit Reads: %lld\n", cnt_lsu_r);
    printf("  Load/Store Unit Writes: %lld\n", cnt_lsu_w);
    printf("  Execution Unit: %lld\n", cnt_exu);
    printf("  R-type Instructions: %lld\n", cnt_inst_r);
    printf("  I-type Instructions: %lld\n", cnt_inst_i);
    printf("  Load Instructions: %lld\n", cnt_inst_l);
    printf("  Store Instructions: %lld\n", cnt_inst_s);
    printf("  U-type Instructions: %lld\n", cnt_inst_u);
    printf("  B-type Instructions: %lld\n", cnt_inst_b);
    printf("  J-type Instructions: %lld\n", cnt_inst_j);
    printf("  CSR Instructions: %lld\n", cnt_inst_csr);
    printf("  System Instructions: %lld\n", cnt_inst_sys);
}