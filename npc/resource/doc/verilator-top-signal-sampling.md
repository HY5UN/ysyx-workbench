# Verilator 顶层信号采样经验（top->rootp-> 通用总结）

> 背景：把 NPC 的性能计数器从「经 dpic 的 DPI-C 上报」改为「C++ 每周期通过 `top->rootp->` 直接读 RTL 信号」。
> 本文把这次改造踩过的坑和结论整理成通用经验，适用于任何想用 Verilator 在 C++ 侧观测顶层 / 内部信号、又想绕开 DPI-C 的场景。

---

## 1. 先理解 Verilator 生成的两类 C++ 类型

- **`VysyxSoCFull`（顶层模块类）** —— 对应 Verilog 顶层模块，是你在 C++ 直接操作的对象：
  - 端口：`top->clock`、`top->reset`；
  - 时序：`top->eval()`、`top->timeInc()`；
  - 波形：`top->trace(tfp)`；
  - 内部指针：`top->rootp`。
  - 一句话：**「可手握、可敲时钟、可 eval」的仿真机**。

- **`VysyxSoCFull___024root`（顶层内部 root 节点类）** —— 存放展开后**扁平化/内部信号**的缓存结构：
  - 通过 `top->rootp`（类型是 `VysyxSoCFull___024root*`）访问；
  - 里面是形如 `ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pf_pfm_begin` 的扁平信号成员；
  - 不提供 `eval()` / `clock`，只是一堆扁平信号的值。
  - 注意 `024` 是 Verilator 生成节点时的固定编号，不要手动改。

```
VysyxSoCFull                    (对外接口: 端口 + eval + trace)
  └── rootp ─────────►  VysyxSoCFull___024root   (扁平化内部信号容器)
                        ├── ...asic__DOT__cpu__DOT__cpu__DOT__pf_pfm_begin
                        └── ...
```

访问内部信号需要 `#include "VysyxSoCFull___024root.h"`；操作仿真仍用 `VysyxSoCFull`。

---

## 2. 核心结论：不带 `--public-flat-rw` 时，只有「寄存器」会被提升到 rootp

这是本次改造**最重要**的发现，也是走弯路的原因。

| 信号类型 | 是否出现在 `top->rootp->`（仅 `--trace-fst`，无 public-flat） |
| --- | --- |
| 纯组合 `Wire`（无黑盒消费） | ❌ 不会，会被合入表达式/优化掉 |
| `RegNext` / `RegEnable` 寄存器 | ✅ 会（寄存器是独立的网表节点，且被 trace） |
| 直连黑盒（如 dpic ExtModule）输入的组合 Wire | ❌ 不会 |
| 经 `RegNext` 再连黑盒输入的寄存器 | ✅ 会（名称常带 `_REG` 之类后缀） |

**推论**：想要「轻量地」让某个内部信号可被 `rootp` 读到，最简单可靠的做法是：**用 `RegNext`(或 `RegEnable`) 把它注册成一个寄存器并 `dontTouch`**，而不是用裸组合 wire，更不是上 `--public-flat-rw`。

---

## 3. 三档方案对比（按编译代价从小到大）

### 方案 A：什么都不加，纯靠 `--trace-fst` 暴露的「一部分」信号
- **代价**：最小（这就是本项目的默认状态）。
- **可见性**：只有 Verilator 恰好保留的那些顶层/寄存器信号，任意的深层 wire 不一定可见。
- **适用**：你需要的信号恰好已暴露；否则不适用。

### 方案 B：`RegNext` + `dontTouch` 把要采样的信号做成寄存器（推荐，本次采用）
- **代价**：小。只多几个寄存器，`--trace-fst` 本身就在。
- **可见性**：`dontTouch` 的 `RegNext` 寄存器会被提升到 `top->rootp->`，精准可控。
- **适用**：需要观测一组有限的内部信号（性能计数、调试探针等）。

### 方案 C：`--public-flat-rw`（或 `--public-flat-rd`）暴露全部信号
- **代价**：**极大**。会把整个设计的几十万信号全部暴露，导致 Verilator 生成的单个 Syms 构造文件巨大，C++ 编译极慢（本项目实测风扇不转、长时间卡在一个约几百 MB 的单文件上）。
- **可见性**：全部信号可读可写（`rw`；`rd` 只读、更轻一点但仍全暴露）。
- **适用**：确实需要访问几乎全部信号、且能接受编译时间；否则避免。

> **教训**：不要为了读十几个信号而开 `--public-flat-rw`。「只暴露需要的 」永远优于「全暴露」。

---

## 4. 信号命名与层级

扁平名遵循 `顶层__DOT__模块__DOT__实例__DOT__...__DOT__信号` 的规则，`__DOT__` 是 Verilator 对 `.` 的转义。以 ysyxSoC 为例：

- SoC 顶层模块名是 `ysyxSoCFull`（不是 `ysmSoCFull`，别拼错——我们踩过一次）。
- CPU 实际层级为 `asic.cpu.cpu`（SoC.asic 里的 CPU 模块再实例化核心 `ysyx_26010036`）。
- 所以核心内部信号是：
  `ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pf_pfm_begin`

**如何拿到准确名字**：不猜，看生成的 `obj_dir/VysyxSoCFull___024root.h`：

```bash
grep -oE "ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pf_[a-zA-Z0-9_]*" build/sim_ysyxsoc/obj_dir/VysyxSoCFull___024root.h | sort -u
```

**命名后缀坑**：`RegNext`/`RegEnable` 生成的寄存器名可能带后缀，且**因是否连了黑盒而不同**：
- 直连黑盒输入时，常是 `dpic_io_xxx_REG`（`_REG`）；
- 独立 `dontTouch` 时，可能是 `pf_xxx`（**不带后缀**）。
务必以实际生成的头文件为准。

---

## 5. 推荐的数据流向 / 代码结构

### RTL（Scala / Chisel）
在核心里把要采样的信号注册成寄存器并 `dontTouch`：

```scala
val pf_pfm_begin = RegNext(ifu.io.out.bits.pc >= "ha0000000".U && ifu.io.out.valid)
val pf_if_miss   = RegNext(ica.io.dpic_miss)
// ... 其余同理
dontTouch(pf_pfm_begin); dontTouch(pf_if_miss); // ...
```

要点：
- 用 `RegNext`（时钟沿采样），这样既被提升到 rootp，又天然和 `posedge clk` 的采样时刻对齐（比 dpic 晚一个沿，聚合计数总量一致）。
- **常量折叠坑**：`RegNext(false.B)` 这类恒定输入会被优化掉、不产生寄存器（实测 `ifu_i_flushed` 就没暴露）。所以固定的信号在 C++ 里直接给常量，别指望从 RTL 读。
- 指令类型这类枚举：`RegNext(wbu.io.in.bits.ctrl.pcit.asUInt)`，用 `asUInt` 转成位宽（Chisel 枚举的 `asUInt(width)` 不可用，会报 `overloaded method apply ... (Width)`）。
- 信号名用独立前缀（如 `pf_`）便于和普通逻辑区分。

### C++
采样函数只在该环境开启时编译：

```cpp
// common.h
#if USE_YSYXSOC
void sample_performance_counters(VysyxSoCFull *top);
#endif

// performance_event.cpp
#if USE_YSYXSOC
#include "VysyxSoCFull___024root.h"
void sample_performance_counters(VysyxSoCFull *top) {
    auto &r = *top->rootp;
    svBit a = r.<TOP>__DOT__...__DOT__pf_pfm_begin;
    // ... 统计逻辑
}
#else
void sample_performance_counters(VysyxSoCFull*) {}
#endif
```

### 主循环调用点
在每个上升沿求值完成后采样：

```cpp
top->clock = 1; top->eval();
#if USE_YSYXSOC
sample_performance_counters(top);
#endif
```

### 多构建（如 SoC vs 纯 NPC）复用：只维护「前缀名」，不要维护两份代码
当同一套核心 RTL 会以不同形式打顶（比如 SoC 是 `asic.cpu.cpu`，纯 NPC 是另一套层级），采样/统计的逻辑是**完全一致**的，唯一的差别是 `rootp->` 扁平名的**前缀**。因为信号的**叶子名**（`pf_*`）来自共享的核心模块，两边 elaborate 时名字天然一致——所以**逻辑写一份，前缀按构建切一行**即可。

```c
/* 两级 ## 展开：先展开参数再做 token 拼接 */
#define CAT2(a, b) a##b
#define CAT(a, b)  CAT2(a, b)

#if USE_YSYXSOC
#  define PF_PFX ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__  /* SoC 层级 */
#else
#  define PF_PFX ysyxSoCFull__DOT__你的层级__DOT__                   /* 纯 NPC 层级, 以各自 ___024root.h 为准 */
#endif

#define PF_SIG(name) CAT(PF_PFX, name)   /* 所有读信号统一走这里 */
```

调用处两边完全一样（仅展开结果不同）：

```c
svBit x = r.PF_SIG(pf_pfm_begin);
/* => r.ysyxSoCFull__DOT__asic__DOT__cpu__DOT__cpu__DOT__pf_pfm_begin   (SoC)
   => r.ysyxSoCFull__DOT__你的层级__DOT__pf_pfm_begin                  (NPC) */
```

两个前提：
1. **叶子名必须一致**——都来自共享核心模块。若某个信号只在一种构建里存在或名字不同，用 `#if` 单独包那几行即可，主体仍共享。
2. **前缀必须精确**——别猜，分别去两个构建的 `obj_dir/VysyxSoCFull___024root.h` 里 grep 确认（`__DOT__` 层数、实例名）。

什么时候才值得拆成两份函数：只有当**两边信号集合/语义明显不同**（如某构建要多读一组完全不同的信号）才拆。否则「读信号」一律走前缀宏，保持单份逻辑。顺便：组合信号（如寄存器文件 `gpr.io.regs` 这类纯组合）可能不会被提升到 rootp，需要先寄存器化 + `dontTouch`。

---

## 6. 其他易踩的坑

1. **BLKANDNBLK**：开 `--public-flat-rw` 后，某个外设里一个变量同时被阻塞/非阻塞赋值会变成 error（如 psram 的 `QPI_enabled`），需要 `-Wno-BLKANDNBLK`。这是**全暴露带来的副作用**，方案 B 不会触发。
2. **`#if USE_YSYXSOC` 作用域**：相关代码要严格包在 `#if USE_YSYXSOC` 里；非 SoC 给空实现，保证另一套构建（如纯 NPC）也能编译过。
3. **宏拼错顶层名**：`ysyxSoCFull` vs `ysmSoCFull` 差一个字符，编译报 `no member named`，直接从生成头文件里复制名字最稳。
4. **类型转换**：读到的 Bool/`CData` 直接赋给 `svBit`/`int` 即可；`CData/*7:0*/` 读成 `char` 时留意宽度。
5. **分层/前缀**：模块实例名不同则路径不同（本项目是两级 `cpu`），改 SoC 布局后要重新查头文件。

---

## 7. 快速验证流程

```bash
# 1. 改完 RTL + C++ 后，先构建（不含仿真），拿到根头文件
make build/sim_ysyxsoc/ysyxSoCFull

# 2. 确认信号确实暴露
grep -o "pf_pfm_begin" build/sim_ysyxsoc/obj_dir/VysyxSoCFull___024root.h

# 3. 跑目标（本项目为 microbench）
make mi-soc
# 观察输出里的 dashboard 是否与期望一致
```

---

## 8. 决策速查表

| 需求 | 推荐做法 |
| --- | --- |
| 读一组有限的内部信号（性能计数 / 探针） | **方案 B**：`RegNext` + `dontTouch`，C++ 用 `top->rootp->` 读 |
| 需要读几乎全部信号 | 方案 C：`--public-flat-rd`（只读，比 rw 轻），需接受编译时间，可能还要 `-Wno-*` |
| 不改 RTL、只读已暴露的少量信号 | 方案 A：直接走 `top->rootp->`，但务必先查头文件确认存在 |
| 完全不碰 Verilator 模型 | 保留 DPI-C（如 dpic）方式 |
