# Repository Guidelines

If the factual descriptions in this guide become outdated after new features are implemented in later conversations, propose a concrete update and apply it only after the user approves. Keep this file under 500 words.

This repository is the ysyx (One Student One Chip) workbench: `npc` is a Chisel/Scala RV32 RISC-V core, `nemu` is a C RV32IM reference emulator, and AbstractMachine (AM) is the bare-metal runtime layer that hosts all applications and OSes.

## Project Structure & Module Organization

- `npc/` - Chisel RTL in `src/main/scala/` (`core/`, `bus/`, `utils/`), a C++ harness in `csrc/`, Scala tests in `test/src/`, and Makefile targets for Verilator/iverilog/Yosys. Build artifacts go under `build/`.
- `nemu/` - C emulator sources under `src/`, configured via `make menuconfig`.
- `abstract-machine/` - AM runtime (`am/`, `klib/`) and per-ARCH build scripts in `scripts/`.
- `am-kernels/` - benchmarks and kernels such as `benchmarks/microbench`, `coremark`, and `dhrystone`.
- `nanos-lite/` and `navy-apps/` - teaching OS and its applications/libraries.
- `rt-thread-am/`, `ysyxSoC/`, `nvboard/`, `fceux-am/` - ports, SoC peripherals, board files, and emulator assets. `patch/` stores patches for the embedded git repos.
- Root `Makefile` implements tracer commits; do not modify it. `init.sh` clones upstream subprojects.

## Build, Test, and Development Commands

- Initialize with `bash init.sh <subproject>`; export `NPC_HOME`, `NEMU_HOME`, `AM_HOME`, `NAVY_HOME`, `NVBOARD_HOME`, or run `make env` in `npc/` to append them to `~/.bashrc`.
- Run any AM app with `make run ARCH=riscv32e-npc [mainargs="..."]`; use `riscv32-nemu` or `riscv32e-ysyxsoc` for other targets.
- From `npc/`: `make sim IMG=image.bin` (Chisel to Verilator), `make sim-ysyxsoc IMG=image.bin`, `make sim-iverilog IMG=image.bin`, `make sim-iverilog-netlist IMG=image.bin`, plus `syn` and `sta`. `make rt` and `make mi` run RT-Thread and microbench on NPC.
- From `nemu/`: `make menuconfig`, `make`, then `make run IMG=image.bin`.

## Coding Style & Naming Conventions

Use two-space indentation in C/C++ and Scala. C uses snake_case; Scala uses PascalCase for classes and bundles, camelCase for signals and ports. Keep builds warning-clean (AM compiles with `-Wall -Werror`). Format Scala with `npc/.scalafmt.conf` and never commit generated binaries or `build/` outputs.

## Testing Guidelines

Scala tests use ChiselTest/ScalaTest in `npc/test/src/`; run `mill -i root.test` from `npc/`. For functional coverage, run AM benchmarks on NPC and compare results against NEMU (`make mi` runs microbench). Use the `sim-iverilog*` targets to validate generated Verilog and netlists.

## Commit & Pull Request Guidelines

History uses short, terse subjects (`fix`, `submit`, `ci submit`). Keep hand-written commits small and descriptive, and never hand-author the auto-generated tracer commits (`> sim RTL - NPC ...`). For PRs, summarize the change, list affected ARCH/platform targets, and include verification commands; attach logs or screenshots for user-visible behavior.
