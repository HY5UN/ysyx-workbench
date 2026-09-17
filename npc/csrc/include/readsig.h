#pragma once
// ============================================================================
// READSIG(CORE, <path...>)  -- 逐层路径扁平化为 Verilator 根层级信号访问
//
// 用法（CORE 之后为逗号分隔的逐层名字，支持任意深度）：
//     READSIG(CORE, ifu, pc)
//        -> (top->rootp->ysyxSoCFull__DOT__core__DOT__ifu__DOT__pc)
//     READSIG(CORE, pf_pfm_begin)
//        -> (top->rootp->ysyxSoCFull__DOT__core__DOT__pf_pfm_begin)
//
// 其中 CORE 是"内核所在层级"的占位宏，随 USE_YSYXSOC 自动切换：
//     USE_YSYXSOC=1 : ysyxSoCFull.asic.cpu.cpu
//     USE_YSYXSOC=0 : ysyxSoCFull.core
//
// 注意：C 预处理器无法把 module1.module2 拆成多个宏实参（只有 ',' 能拆，
//       '##' 也不能跨 '.' 拼接），因此逐层路径必须用逗号分隔，而不能用 '.'。
//       需要 top->rootp 可用（包含 VysyxSoCFull___024root.h）。
// ============================================================================

// ---- CORE：内核所在层级，随 USE_YSYXSOC 自动切换 ----
#if USE_YSYXSOC
#define CORE ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu
#else
#define CORE ysyxSoCFull__DOT__core
#endif

// ---- 内部拼接工具（两段式，确保参数先展开再粘贴） ----
#define READSIG_CONCAT(a, b)   READSIG_CONCAT_(a, b)
#define READSIG_CONCAT_(a, b)  a##b
#define READSIG_DOT            __DOT__

// ---- 递归折叠：把 (acc, x) 接成 acc__DOT__x，逐层剥完剩余实参 ----
#define READSIG_FOLD_1(acc, x)                READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x))
#define READSIG_FOLD_2(acc, x, ...)           READSIG_FOLD_1(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_3(acc, x, ...)           READSIG_FOLD_2(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_4(acc, x, ...)           READSIG_FOLD_3(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_5(acc, x, ...)           READSIG_FOLD_4(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_6(acc, x, ...)           READSIG_FOLD_5(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_7(acc, x, ...)           READSIG_FOLD_6(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_8(acc, x, ...)           READSIG_FOLD_7(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_9(acc, x, ...)           READSIG_FOLD_8(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_10(acc, x, ...)          READSIG_FOLD_9(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_11(acc, x, ...)          READSIG_FOLD_10(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_12(acc, x, ...)          READSIG_FOLD_11(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_13(acc, x, ...)          READSIG_FOLD_12(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_14(acc, x, ...)          READSIG_FOLD_13(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_15(acc, x, ...)          READSIG_FOLD_14(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)
#define READSIG_FOLD_16(acc, x, ...)          READSIG_FOLD_15(READSIG_CONCAT(acc, READSIG_CONCAT(READSIG_DOT, x)), __VA_ARGS__)

// ---- 根据实参个数选择对应的 FOLD 层数（最多 16 层，需要更多自行扩展） ----
#define READSIG_GET_FOLD(_1,_2,_3,_4,_5,_6,_7,_8,_9,_10,_11,_12,_13,_14,_15,_16,NAME,...) NAME
#define READSIG_FOLD(acc, ...) READSIG_GET_FOLD(__VA_ARGS__, READSIG_FOLD_16, READSIG_FOLD_15, READSIG_FOLD_14, READSIG_FOLD_13, READSIG_FOLD_12, READSIG_FOLD_11, READSIG_FOLD_10, READSIG_FOLD_9, READSIG_FOLD_8, READSIG_FOLD_7, READSIG_FOLD_6, READSIG_FOLD_5, READSIG_FOLD_4, READSIG_FOLD_3, READSIG_FOLD_2, READSIG_FOLD_1)(acc, __VA_ARGS__)

// ---- 对外宏：READSIG(CORE, path...) -> (top->rootp->CORE__DOT__path__DOT__...) ----
#define READSIG(...)  (top->rootp-> READSIG_FOLD(__VA_ARGS__))
