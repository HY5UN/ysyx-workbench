# ysyx-workbench

One Student One Chip (ysyx) workbench: a RISC-V processor written in Chisel, together with the
reference emulator, the bare-metal runtime, and the applications used to verify it.

## Directory layout

```
.
├── npc/                # The processor core: Chisel RTL, C++ harness, tests, synthesis
├── nemu/               # NEMU: RV32IM+CSR reference emulator used by DiffTest
├── abstract-machine/   # AbstractMachine: klib and bare-metal runtime (am/, klib/)
├── am-kernels/         # Benchmarks and kernels (microbench, coremark, dhrystone, ...)
├── ysyxSoC/            # SoC integration: AXI4, SDRAM controller, peripherals
└── patch/              # Patches applied to the embedded sub-repositories
```

The sub-projects are upstream trees kept in sync through `init.sh` and `patch/`.
