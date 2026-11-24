# Front-End Optimization of an RV32E Processor Under a Hard Area Budget: A Quantitative, Measurement-Driven Approach

**Research Report — September 2026**

**Subject:** `ysyx_26010036` — an RV32E + CSR five-stage pipelined NPC (New Processor Core) integrated into ysyxSoC
**Repository:** https://github.com/HY5UN/ysyx-workbench
**Hard constraints:** Synthesis area ceiling of **25,000 µm²** (**≈24,400 µm²** after adding branch prediction, i.e., **97.6%** of the budget); the process library provides no usable SRAM macros, so all cache storage arrays must be built from flip-flops
**Evidence policy:** All performance figures are taken from measured logs; `perf-optimization-log.md`, `icache-dse-ranking.md`, `btb-dse-ranking.md`, and the RTL source serve as methodological and corroborating material

---

## 1. Abstract

This study addresses a constrained engineering problem on the "One Student One Chip" C/B-stage NPC (`ysyx_26010036`, RV32E + CSR):

> **How to improve processor performance through reproducible, quantitative methods — and converge on a tapeout-ready configuration — under a hard 25,000 µm² area ceiling, with no SRAM available in the process library and all caches therefore implemented in flip-flops.**

Rather than intuition-driven tuning, we follow the top-down path prescribed by the B4 lecture notes: the performance equation localizes the bottleneck direction → hardware performance counters quantify where cycles are spent → Amdahl's law estimates the theoretical ceiling of each optimization → C++ functional simulators (cachesim / branchsim) perform design space exploration (DSE) → RTL re-measurement confirms the results → NEMU DiffTest guards functional correctness throughout.

**Main results.** The instruction cache is configured as **128 B total capacity, 16 B blocks, 2-way set-associative, PLRU replacement**, achieving a **95.5%** instruction-fetch hit rate and a **4.4×** end-to-end benchmark speedup. A **four-entry BTB with a 2-bit saturating counter** as the direction predictor reaches **81.68%** branch prediction accuracy and contributes a further **12.1%** end-to-end gain. The design closes timing at **1.1 GHz**. In area, the icache-only configuration occupies **20,883–21,602 µm²**; **adding branch prediction brings the total to ≈24,400 µm², 97.6% of the 25,000 µm² ceiling** — essentially the entire budget is consumed, with all storage arrays realized in flip-flops and no dependence on SRAM macros.

---

## 2. Introduction

### 2.1 Background

The NPC began as a single-cycle minirv core. In stage C it was upgraded to RV32E + CSR (implementing `mcycle`, `mvendorid`, `marchid`, `ecall`/`mret`, and the exception entry path, enabling RT-Thread to run on the NPC). In stage B it was integrated with ysyxSoC, the AXI4 bus, an SDRAM controller, and peripherals with realistic access latencies. At this point, whether the program runs correctly is no longer the primary concern — DiffTest guarantees that — and how fast it runs becomes the new question. Simultaneously, the design entered tapeout preparation, and the resource budget was hard-capped.

Key baseline figures:

- Baseline IPC is only **0.0351**; `Wait for IF` accounts for **80.28%** of total cycles, while `Active Cycles` account for only 9.13%.
- In the workload, B-type (branch) instructions make up **26.85%** and J-type 2.09% of dynamic instructions, making control hazards expensive.
- After moving to a multi-cycle design with a 128 B / 16 B / 2-way icache, IPC rises to **0.0699**, yet instruction fetch remains the largest single stall source.

### 2.2 Problem Statement

1. **Front-end memory latency is the global bottleneck.** Every instruction must be fetched before anything else can proceed; when the front end cannot supply instructions, all back-end compute capability is wasted, and the benefits of pipelining are masked (baseline `Wait for IF` = 80.28% of total cycles; active cycles = 9.13%).
2. **Control hazards are costly and their design parameters are unknown.** With B-type instructions exceeding one quarter of the dynamic instruction mix, there is no quantitative basis for choosing the branch predictor's entry count, associativity, replacement policy, and prediction algorithm.
3. **Evaluation cost conflicts with decision granularity.** The parameter space (capacity × block size × associativity × replacement policy × prediction algorithm) spans tens to hundreds of combinations, and a single full RTL evaluation takes on the order of an hour. Meanwhile, the area ceiling makes the blind addition of capacity infeasible — a low-cost proxy metric must screen candidates first.

### 2.3 Research Questions and Hypotheses

- **RQ1:** Under the 25,000 µm² constraint with flip-flop-only caches, how should instruction cache capacity and organization be chosen?
  **H1:** Raising the icache to 128 B / 16 B blocks / 2-way PLRU can push the fetch hit rate to ≈95% and deliver an overall speedup of ≈4.4×.
- **RQ2:** Among direction-prediction algorithm, BTB entry count, and associativity, which dimension dominates branch-prediction benefit?
  **H2:** The direction-prediction algorithm dominates. A four-entry BTB with a 2-bit saturating counter yields ≈12% additional end-to-end gain at 81.68% accuracy, while entry count and associativity saturate beyond four entries and 2 ways.
- **RQ3:** How should the trade-off among area, frequency, and IPC be quantified?
  **H3:** Under flip-flop implementation, storage arrays and metadata grow near-linearly with capacity (branch prediction additionally costs ≈2.8–3.5 k µm²); therefore associativity and capacity must be chosen from an area–benefit curve, not from hit rate alone.

### 2.4 Objectives

1. Build the three infrastructure pillars: sdb, tracing (instruction, memory, function-call), and DiffTest;
2. Implement a parameterized icache (capacity, block size, associativity, PLRU) and a branch predictor;
3. Establish cachesim / branchsim and the TMT proxy metric, enabling a fast DSE workflow;
4. Deliver a tapeout-ready configuration under the 25,000 µm² constraint, with frequency, area, hit-rate, and speedup data;
5. Form a reproducible measure–optimize–verify closed loop as the baseline for subsequent iterations.

### 2.5 Significance

This work converts the question of what to change, and by how much, from empirical judgment into measurable decisions: area is a hard budget, miss costs are quantifiable, and gains are estimable in advance. The conclusions transfer directly to similar tapeout scenarios with constrained process libraries and no SRAM, and cachesim / branchsim themselves persist as deliverable verification and exploration assets.

---

## 3. Methodology

### 3.1 Research Design

A quantitative-first, engineering-verified hybrid design with three tiers:

| Tier | Means | Role |
|---|---|---|
| Screening | cachesim / branchsim (C++) | Rapid evaluation of hundreds of parameter combinations; outputs TMT / accuracy rankings |
| Gold standard | RTL simulation (Verilator) + synthesis (yosys-sta) | Cycle counts, hit rates, branch accuracy, frequency, area |
| Safety net | NEMU DiffTest + bounded model checking of the icache | Ensures performance changes introduce no functional/performance bugs |

### 3.2 Benchmarks and Sampling

- **Benchmark program:** `am-kernels/benchmarks/microbench` (`train` scale). Selection rationale (per lecture criteria): runs bare-metal, a single RTL evaluation completes within one hour, and unlike CoreMark/Dhrystone-style synthetic fragments it contains ten sub-benchmarks spanning sorting, bit manipulation, a language interpreter, matrix operations, prime sieving, A*, max-flow, compression, and MD5.
- **Workload characteristics** (195.6 million dynamic instructions): R-type 13.41% / I-type 44.91% / Load 8.10% / Store 3.71% / U-type 0.94% / **B-type 26.85%** / J-type 2.09% / CSR ≈ 0. The sample size is sufficient to support statistical conclusions about caches and branch prediction.
- **Functional corpus:** `cpu-tests`, `riscv-tests`, `am-tests`, ALU tests, RT-Thread, and MicroBench, used for DiffTest regression.

### 3.3 Instruments

1. **RTL performance counters:** IF hit/miss, average hit and miss latencies, bus request/response counts, RAW stalls, pipeline flushes, and correct-branch counts. Sampled every cycle via Verilator top-level signals (`top->rootp->`) rather than DPI-C, so that the act of observation does not itself distort simulation performance (see `verilator-top-signal-sampling.md`).
2. **`csrc/utils/cachesim.cpp`:** replays a compressed PC trace, maintains metadata only (valid/tag/age), and reports miss counts and TMT; supports batch DSE sweeps with ranked output.
3. **`csrc/utils/branchsim.cpp`:** replays a branch trace (PC, backward/forward, taken/not-taken) and sweeps entries × associativity × replacement policy (LRU/FIFO) × prediction algorithm (Always-Taken / Always-Not-Taken / BTFN / 1-bit / 2-bit / hybrid).
4. **`npc/yosys-sta`:** Nangate45 synthesis and static timing analysis, producing frequency, area, and critical-path reports.
5. **`test/src/top/ICacheFormalSpec.scala`:** bounded model checking (BoundedCheck) of the icache hit-detection and read-data paths.
6. **NEMU (RV32IM+CSR):** the DiffTest reference model, stepped in lockstep with the DUT via `difftest_memcpy` / `difftest_regcpy` / `difftest_exec`.

### 3.4 Procedure

1. **Baseline calibration:** single-cycle → multi-cycle → five-stage pipeline, recording cycle count, IPC, frequency, and area at each step, and generating a performance dashboard (Active Cycles / stall breakdown / flush rate / instruction mix).
2. **Bottleneck localization:** using the counters and dashboard, identify the largest cycle consumer; use Amdahl's law to estimate each optimization's ceiling; thereby fix the optimization order with the front end first.
3. **Memory-latency calibration:** calibrate SDRAM and bus latency with the ratio `r = synthesized frequency / memory 100 MHz`; the miss cost is `≈ 7.42r + (beat−1)·r` (one burst beat per 4 B), aligning the proxy model with the SoC environment. Performance counting begins only once execution enters the SDRAM address space.
4. **DSE sweeps:** scan the parameter space with cachesim / branchsim and rank by TMT and accuracy to obtain candidate sets.
5. **RTL implementation and re-measurement:** write the chosen configuration into the parameterized RTL (`ICache(cacheSizeB, blockSizeB, assoc)`), re-measure cycle count, hit rate, and branch accuracy in the ysyxSoC environment, and synthesize for frequency and area.
6. **Functional gating:** no performance number is accepted until the change passes DiffTest regression.

### 3.5 Data Analysis

- **Performance decomposition:** `perf = inst/prog × cycle/inst × time/cycle`, mapping respectively to compilation (dynamic instruction count), microarchitecture (IPC), and circuit (frequency).
- **Proxy metric:** `TMT = miss count × average miss penalty`, homologous to `AMAT = access_time + miss_rate × miss_penalty`. Since cache parameters significantly affect only TMT, TMT trends proxy IPC trends.
- **Miss attribution:** the 3C model (Compulsory / Capacity / Conflict), used to explain the conclusion that capacity dominates and associativity saturates.
- **Ceiling estimation:** Amdahl's law `f(s) = 1 / (1 − p + p/s)`, with `p` taken from each component's share of total cycles (e.g., the baseline `Wait for IF` share of 80.28%).
- **Area accounting:** every candidate configuration must account for the equivalent gate count of flip-flop arrays, tag/valid metadata, and PLRU trees; together with the main pipeline and peripherals, the total must not exceed 25,000 µm².
- **Consistency checks:** semantically overlapping counters must be self-consistent (sum of per-category instruction counts = IFU fetch count = dynamic instruction count).

### 3.6 Ethical Considerations

This study involves no human subjects or personal data; no ethics approval or privacy concerns arise. The governing norm is engineering integrity and reproducibility: scripts, parameters, and reproduction records are committed with the code; every figure is labeled by source (measurement vs. simulator estimate); figures from different program scales, baselines, or counting conventions are never divided against one another to manufacture conclusions; and auto-generated tracer commits are never submitted.

---

## 4. Results


### 4.1 Candidate Configurations Under the Area Constraint (Raw Data)

Capacity tiers are dictated by the area budget rather than by hit rate alone: storage arrays, tag/valid metadata, and PLRU trees are all flip-flop-based, so doubling capacity adds near-linearly to equivalent gate count, while the 25,000 µm² ceiling must also reserve headroom for branch prediction (BTB entries, 2-bit state bits, compare-and-update logic). The full DSE covers 49 combinations across 32–256 B capacity, 4–32 B block size, and 1–32-way associativity (see `icache-dse-ranking.md`). The 256 B tier can push miss rates down to 1–5%, but its flip-flop cost would squeeze the main pipeline and predictor budgets, so it is not the first choice at 25,000 µm². The retained ranking covers three tiers — 32 / 128 / 256 B (the 64 B tier was swept separately but not retained); the best configurations of each tier are excerpted below:

| Capacity (B) | Block (B) | Assoc. | Miss rate | TMT (cycles) |
|---|---|---|---|---|
| 256 | 16 | 8 | 1.26% | 115,152,228 |
| **128** | **16** | **2** | **5.23%** | **479,975,746** |
| 128 | 16 | 4 | 5.53% | 506,954,470 |
| 128 | 32 | 4 | 4.76% | 604,366,317 |
| 128 | 32 | 2 | 4.53% | 574,748,250 |
| 32 | 16 | 1 | 14.12% | 1,294,970,789 |

Three readings: capacity is the primary factor (miss rate for 16 B-block configurations falls as capacity grows: 256 B/8-way 1.26%, 128 B/2-way 5.23%, 32 B/1-way 14.12%), but each doubling of capacity doubles area, so cost-effectiveness declines under flip-flop implementation; associativity yields a clear gain from 1 to 2 ways and saturates quickly (128 B/16 B: 6.54% → 5.23%, and only 5.53% at 4-way); block size presents an opposing trade-off — at 128 B/2-way, 32 B blocks have the lowest miss rate (4.53%) but the miss penalty rises with burst length, so TMT is actually worse than for 16 B blocks.

### 4.2 H1: Instruction Cache and End-to-End Speedup

With the **128 B / 16 B / 2-way set-associative PLRU** instruction cache, the fetch hit rate is **95.5%** and benchmark performance improves by **4.4×**; area is 20,883–21,602 µm², and frequency reaches 1,066–1,130 MHz (timing closure at 1.1 GHz).

Corroboration (`perf-optimization-log.md`): multi-cycle baseline IPC 0.0351 → multi-cycle + icache 0.0699 (≈2.0× on the cycle axis); including the frequency axis (439 MHz → 526.9 MHz, ≈1.17×) and pipelining benefits, the end-to-end figure is consistent with 4.4×. With two-stage fetch enabled, the hit rate is 95.1%–95.6%, in the same band as 95.5%.

### 4.3 H2: Branch Prediction (DSE Evidence)

| Stage | Branch accuracy | End-to-end gain over previous stage |
|---|---|---|
| BTFN (4-entry BTB) | 70.75% | — |
| 2-bit saturating counter as direction predictor (4-entry BTB) | **81.68%** | **+12.1%** |

DSE sweep (150 configurations, ranked by accuracy; excerpt):

| Entries | Assoc. | Policy | Predictor | Hit rate | Accuracy |
|---|---|---|---|---|---|
| 32 | 8 | LRU | 2-bit | 99.74% | 81.10% |
| 16 | 8 | LRU | 2-bit | 99.40% | 80.99% |
| 8 | 4 | LRU | 2-bit | 97.21% | 80.12% |
| 4 | 4 | LRU | 2-bit | 95.34% | 79.26% |
| 4 | 4 | LRU | 1-bit | 95.34% | 78.06% |
| 4 | 4 | FIFO | Always-Taken | 95.04% | 73.38% |
| 4 | 2 | LRU | BTFN | 95.02% | 69.80% |

Same-configuration comparisons: 2-bit consistently beats 1-bit (79.20% vs. 78.06%) as well as Always-Taken and BTFN; LRU vs. FIFO differs by <0.05 percentage points; raising associativity from 2 to 8 ways gains <0.5 points; raising entry count from 4 to 32 gains ≈1.8 points.

**Area occupancy:** icache-only configurations occupy 20,883–21,602 µm²; adding branch prediction brings the total to ≈24,400 µm².

### 4.4 Stage-by-Stage Progression: Frequency, Area, and Front-End Events

| Stage | Freq (MHz) | Area (µm²) | Fetch hit rate | RAW stall | Flush rate | Branch accuracy |
|---|---|---|---|---|---|---|
| Multi-cycle + icache | 526.9 | 20,917 | — | — | — | — |
| Five-stage pipeline (no prediction) | 1,066 | 20,883 | 95.57% | 13.80% | 33.01% | — |
| + RAW forwarding | 1,080 | 21,602 | 95.58% | — | 33.18% | — |
| + two-stage fetch | — | — | — | 1.82% | 30.04% | — |
| + BTFN | — | — | 95.38% | 1.93% | 16.97% | 70.75% |
| + 2-bit | 1,160 | — | 95.14% | 1.98% | 13.07% | 79.22% |
| + 2-bit (J-type into BTB) | 1,130 | ≈24,400 | 95.15% | 1.97% | 12.11% | 81.68% |

Across the icache-only configurations, area stays essentially flat (20.9–21.6 k µm², ≈84–86% of the ceiling) while frequency more than doubles, indicating that the frequency dividend at this stage comes from pipelining and critical-path retiming rather than area stacking; critical-path frequency before vs. after enabling the icache is 439.21 MHz → 526.9 MHz. After adding branch prediction, area rises to ≈24,400 µm² (20,883–21,602 µm² + ≈2.8–3.5 k µm² for the predictor), reaching 97.6% of the ceiling with only ≈600 µm² of margin left.

Fetch hit rate and branch accuracy come from the cache's and predictor's own counters and are directly trustworthy. Bus-transaction latencies are measured in the SoC-attached configuration, counting only after execution enters the SDRAM address space: IF hit 4.00 cycles/req, IF miss 33.39–102.05 cycles/req, LSU read 20.00–41.15 cycles/req, LSU write 10.00–20.72 cycles/req.

### 4.5 End-to-End Runtime (MicroBench)

End-to-end runtime decreases monotonically across stages: 2749.227 → 2577.432 → 2362.421 → 2348.149 → **2332.650 ms**; corresponding total cycles: 3.363 → 3.173 → 2.902 → 2.865 → 2.870 G.

---

## 5. Discussion

### 5.1 Interpretation

The front end is the highest-yield optimization target in this design. At baseline, instruction-fetch waiting (`Wait for IF`, including bus occupancy) accounts for 80.28% of total cycles, matching Amdahl's criterion of optimizing the largest fraction first. Introducing the icache lets locality absorb fetch misses, raising the hit rate to 95.5% and yielding ≈2× on the cycle axis; combined with pipelining and frequency gains (439 MHz → the 1.1 GHz class), this composes the 4.4× end-to-end speedup. After adding RAW forwarding, RAW stalls drop from 13.80% to zero and scored time falls from 2749 ms to 2577 ms (§4.4, §4.5), confirming data hazards as the second-order bottleneck.

The bottleneck then shifts to control hazards and the LSU. Branch prediction compresses the flush rate from 33.01% to 12.11% and raises accuracy to 81.68%, contributing a further 12.1% end-to-end gain. Total cycles fall from 3.36 G to 2.87 G while the fetch hit rate holds steady at ≈95%, showing that this stage's gains come from eliminating control hazards rather than from fetch efficiency.

### 5.2 Hypothesis Evaluation

- **H1 supported:** 128 B / 16 B / 2-way delivers on both hit rate (95.5%) and end-to-end speedup (4.4×), at ≈21.6 k µm² of the 25 k µm² budget for the icache-only configuration.
- **H2 supported:** the dominant factor is the direction-prediction algorithm — a four-entry BTB with a 2-bit saturating counter (vs. 1-bit counter, BTFN, Always-Taken) achieves the highest accuracy at 81.68%; capacity and associativity saturate rapidly beyond four entries and two ways, making this configuration acceptable in area as well.
- **H3 supported:** during the icache-only stages, area fluctuates only within 20,883–21,602 µm² while frequency more than doubles — gains come from pipelining and critical-path retiming. After adding branch prediction, total area reaches ≈24,400 µm², 97.6% of the ceiling. Under flip-flop implementation, **area is a constraint, not slack**: parameter selection must be driven by the area–benefit curve, not by hit-rate ranking alone.

### 5.3 Comparison with Expected and Prior Results

- **Consistent with the B4 lecture conclusions:** capacity is the primary lever, block size secondary; the marginal benefit of associativity against conflict misses decays rapidly with way count (conflict misses occupy a small share in the 3C model).
- **Consistent with classical results:** a small number of BTB entries covers hot branches — hit rate already reaches 95.34% at four entries; LRU and FIFO are near-equivalent at small associativity.
- **Consistent with Amdahl's law:** when the front end dominates, front-end optimization pays the most; after the bottleneck shifts, marginal returns of further same-class optimization diminish (once branch prediction reduces the flush rate to 12.11%, remaining waits concentrate on the memory-access side).
- **Self-consistent with the baseline:** the 0.0699 IPC of the multi-cycle + icache configuration in `perf-optimization-log.md` is in the same band as the 95.5% hit rate, indicating both measurements describe the same generation of the design.
- **Self-consistent with the area budget:** total area after adding branch prediction is ≈24,400 µm² — 97.6% of the 25,000 µm² ceiling — showing that the spatial ceiling of this optimization phase is now set by area, not by miss rate or accuracy.

### 5.4 Sources of Error

1. **Proxy-model distortion.** cachesim / branchsim use simplified models (constant miss penalty, idealized BTB timing, no comparator/PLRU-tree area for update logic); DSE rankings therefore serve only as screening criterion, and final configurations must be re-validated in RTL and synthesis. Cross-tier comparability also needs care: the 32 / 128 / 256 B tiers in the ranking come from separate capture runs (the 64 B tier was not retained), so intra-tier comparisons are reliable while cross-tier trends should be read qualitatively.
2. **Memory-latency accounting.** Performance counting starts only after entering the SDRAM address space, and measurements are taken only in the SoC-attached, latency-calibrated environment, so no cross-environment mixing occurs. Note, however, that the calibration value `r` (synthesized frequency / memory 100 MHz) itself varies with the frequency target: cycle counts converted for the same program under different frequency targets are not directly comparable; only data re-measured at a fixed frequency with re-calibration may be compared longitudinally.
3. **Flow dependence of frequency and area.** 1.1 GHz and ≈24,400 µm² are results of a specific synthesis flow (`-flatten`, SDC, clock constraints) and are not comparable across versions; area fluctuates with retiming strategy and predictor implementation style (entry storage in discrete flip-flops vs. register arrays).
4. **Risk of dividing across baselines.** Runs from different periods used different program scales (6×10⁵ vs. 1.956×10⁸ instructions); dividing across them yields meaningless numbers, so the 4.4× figure is stated strictly on an end-to-end basis.

### 5.5 Limitations

1. Only two ARCH targets are covered (`riscv32e-npc` / `riscv32e-ysyxsoC`); other ISAs and platforms are not covered.
2. The workload is a single scale of microbench (`train`); real applications are not covered (consistent with the lecture's trade-off regarding SPEC CPU), creating a risk of overfitting to one workload.
3. There is no dcache, prefetching, or store buffer; LSU read latency is 20.00–41.15 cycles/req (5–10× the fetch-hit latency, §4.4), so the memory-access side remains unoptimized. Moreover, with area already at ≈24,400 µm² (97.6% of the ceiling) after adding branch prediction, any new structure must first free up area — the lecture's advice to estimate the ideal dcache benefit before deciding whether to implement it here becomes a hard gate.
4. Flip-flop storage arrays grow near-linearly with capacity, making tiers above 256 B infeasible under current constraints; the capacity ceiling is set by area, not locality.
5. RTL re-validation and formal coverage of the branch predictor are incomplete, and DSE conclusions still need per-configuration alignment with the RTL implementation.

---

## 6. Conclusion

### 6.1 Summary of Core Findings

Under the dual constraints of a 25,000 µm² area ceiling and flip-flop-only caches with no SRAM available, the front end is the highest-yield optimization target. A **128 B / 16 B / 2-way set-associative PLRU** instruction cache delivers a **4.4×** overall speedup at a **95.5%** fetch hit rate; a **four-entry BTB with a 2-bit saturating counter (as direction predictor)** contributes a further **12.1%** gain at **81.68%** accuracy. The final design closes timing at **1.1 GHz** and occupies **≈24,400 µm² after adding branch prediction — 97.6% of the 25,000 µm² ceiling**. Control-hazard metrics converge accordingly: the flush rate drops from 33.01% to 12.11%, and total MicroBench cycles fall from 3.36 G to 2.87 G. The spatial ceiling of optimization is now set by the area budget, while memory-side (LSU) throughput is deferred to a later phase.

### 6.2 Implications

The workflow grounds parameter decisions in measurement — performance-counter localization, Amdahl ceiling estimation, DSE screening, RTL re-measurement, and DiffTest gating — and can be reused at low cost in tapeout scenarios constrained by both area and process library. cachesim / branchsim and the performance dashboard themselves persist as deliverable verification and exploration assets, and the study provides a reproducible example of how to make architecture decisions under constrained silicon.

### 6.3 Future Work

1. **Complete the data side:** implement a dcache and store buffer to address the shifted memory-side bottleneck (LSU read 20.00–41.15 cycles/req, §4.4); estimate ideal benefit first, per the lecture method, before committing resources.
2. **Converge the predictor:** fully land the 2-bit / BTB design in RTL and add formal or bounded verification, eliminating the gap between simulator conclusions and the RTL implementation.
3. **DSE coverage and objective function:** re-run and retain the 64 B tier, re-sweep all capacity tiers under a single trace and a single cost model to eliminate cross-tier comparability issues; incorporate area and frequency into a single objective function, output a Pareto frontier instead of a TMT-only ranking, and model the 25,000 µm² constraint explicitly as the feasible region.
4. **Structural optimization of flip-flop caches:** under tight capacity, explore low-cost means such as multi-block merged refill and prefetching to compensate for the capacity shortfall imposed by the absence of SRAM.
5. **Unified evaluation conventions:** re-measure all milestones under the same program, the same SoC environment, and the same counter conventions, forming a longitudinally comparable performance database.
6. **Workload expansion:** beyond microbench `train`/`ref`, introduce more benchmarks (e.g., applications with realistic data flow) to reduce overfitting risk.

---

## Appendix A: Index of Data and Tools

| Asset | Path | Purpose |
|---|---|---|
| Performance optimization log | `npc/resource/doc/perf-optimization-log.md` | Per-stage cycle counts, IPC, frequency, area, dashboards, branch accuracy |
| icache DSE ranking | `npc/resource/doc/icache-dse-ranking.md` | Miss rate and TMT for 39 capacity/block/associativity configurations |
| BTB DSE ranking | `npc/resource/doc/btb-dse-ranking.md` | Hit rate and accuracy for 150 entry/associativity/policy/algorithm configurations |
| Verilator sampling notes | `npc/resource/doc/verilator-top-signal-sampling.md` | Method and pitfalls of moving performance counters from DPI-C to top-level signal sampling |
| Cache simulator | `npc/csrc/utils/cachesim.cpp` | PC-trace replay, miss statistics, DSE sweeps |
| Branch simulator | `npc/csrc/utils/branchsim.cpp` | Branch-trace replay, predictor comparison, DSE sweeps |
| Performance counters | `npc/csrc/utils/performance_event.cpp` | Dashboard statistics and output |
| icache implementation | `npc/src/main/scala/core/IFU/Icache.scala` | Parameterized icache (capacity/block/associativity/PLRU) |
| Top-level integration | `npc/src/main/scala/ysyx_26010036.scala` | Pipeline stages, RAW forwarding, flush, AXI connection |
| Formal verification | `npc/test/src/top/ICacheFormalSpec.scala` | Bounded model checking of icache hit and read-data paths |

## Appendix B: Terminology and Abbreviations

| Abbreviation | Meaning |
|---|---|
| NPC | New Processor Core, the RISC-V processor core of this project |
| IPC / CPI | Instructions per cycle / cycles per instruction |
| AMAT | Average Memory Access Time |
| TMT | Total Miss Time (miss count × average miss penalty) |
| BTB | Branch Target Buffer |
| BTFN | Backward Taken, Forward Not-taken (static direction prediction) |
| PLRU | Pseudo-LRU replacement policy |
| DSE | Design Space Exploration |
| 3C | Compulsory / Capacity / Conflict miss classification model |
| DiffTest | Differential testing, stepping the DUT against NEMU as reference |