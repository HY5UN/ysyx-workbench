# ==============================================================================
# 全局配置
# ==============================================================================
TOPNAME = ysyxSoCFull
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)
YSYXSOC_DIR = $(abspath ../ysyxSoC)

YOSYS_STA_DIR ?= $(abspath ./yosys-sta)
CLK_FREQ_MHZ ?= 1000
CLK_PORT_NAME ?= clock
SDC_FILE ?= $(YOSYS_STA_DIR)/scripts/default.sdc

# ==============================================================================
# 切换流程：1、改变开关  2、改变ifu初始pc (如果开启difftest:用新配置编译nemu)
# ==============================================================================
USE_YSYXSOC ?= 1

USE_NVBOARD ?= 1 # NVBoard开关：当前仅支持开启soc时使用

ENABLE_DPIC ?= 1

.SECONDARY:
-include ../Makefile

# ==============================================================================
# Chisel 构建 (Mill)
# ==============================================================================


verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
ifeq ($(USE_YSYXSOC),1)
	ENABLE_DPIC=$(ENABLE_DPIC) ./mill -i runMain ElaborateSOC --target-dir $(BUILD_DIR)
else
	ENABLE_DPIC=$(ENABLE_DPIC) ./mill -i runMain Elaborate --target-dir $(BUILD_DIR)
endif


clean:
	-rm -rf $(BUILD_DIR)

.PHONY: verilog clean

# ==============================================================================
# NVBoard 接入
# ==============================================================================
include $(NVBOARD_HOME)/scripts/nvboard.mk

ifeq ($(USE_NVBOARD),1)

ifeq ($(NVBOARD_HOME),)
$(error NVBOARD_HOME未设置，请先 export NVBOARD_HOME=<nvboard项目路径>)
endif

NXDC_FILES ?= $(abspath ./constr/$(TOPNAME).nxdc)

SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	@mkdir -p $(dir $@)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@


endif

# ==============================================================================
# 源文件
# ==============================================================================

CORE_VSRCS = $(filter-out \
  %/ysyxSoCFull.v %/ysyxSoCFull.sv, \
  $(wildcard $(abspath $(BUILD_DIR))/*.v) \
  $(wildcard $(abspath $(BUILD_DIR))/*.sv))

VSRCS = \
  $(CORE_VSRCS) \
  $(shell find $(abspath ./src/main) \( -name "*.v" -o -name "*.sv" \))

ifeq ($(USE_YSYXSOC),1)
VSRCS += $(shell find $(YSYXSOC_DIR)/perip -name "*.v")
VSRCS += $(YSYXSOC_DIR)/build/ysyxSoCFull.v
else
VSRCS += $(abspath $(BUILD_DIR)/ysyxSoCFull.sv)
endif

CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -o -name "*.cc" -o -name "*.cpp")

ifeq ($(USE_NVBOARD),1)
CSRCS += $(SRC_AUTO_BIND)
endif

# ==============================================================================
# Verilator 编译配置
# ==============================================================================

VERILATOR_FLAGS += --cc --exe --build \
                   --Mdir $(OBJ_DIR) \
                   -o $(abspath $(BIN)) \
                   --top-module $(TOPNAME) \
                   -I$(abspath $(BUILD_DIR)/verification) \
                   -I$(YSYXSOC_DIR)/perip/uart16550/rtl \
                   -I$(YSYXSOC_DIR)/perip/spi/rtl \
                   --timescale "1ns/1ns" \
                   --no-timing \
                   --trace-fst \
                   --autoflush

VERILATOR_FLAGS += -j $(shell nproc) \
                   -O3 \
                   --output-split 20000

# VERILATOR_FLAGS += --threads 8

VERILATOR_CFLAGS  += $(addprefix -CFLAGS , $(CXXFLAGS)) \
                     $(addprefix -CFLAGS , -I$(abspath ./csrc)) \
                     -CFLAGS -DUSE_YSYXSOC=$(USE_YSYXSOC) \
                     -CFLAGS -DUSE_NVBOARD=$(USE_NVBOARD)

ifeq ($(USE_NVBOARD),1)
VERILATOR_CFLAGS += $(addprefix -CFLAGS -I, $(INC_PATH))
endif

LDFLAGS           += -lreadline
VERILATOR_LDFLAGS += $(addprefix -LDFLAGS , $(LDFLAGS))

# ==============================================================================
# 仿真规则
# ==============================================================================

default: sim

vsim: verilog sim

sim: $(if $(filter 1,$(USE_NVBOARD)),$(NVBOARD_ARCHIVE) $(SRC_AUTO_BIND))
	$(call git_commit, "sim RTL")
	@echo "--- Building $(TOPNAME) with Verilator ---"
	@mkdir -p $(OBJ_DIR)
	@if [ -z "$(VSRCS)" ]; then \
		echo "Error: No .v/.sv files found. Run 'make verilog' first."; \
		exit 1; \
	fi
	verilator $(VERILATOR_FLAGS) \
		$(VSRCS) $(CSRCS) $(if $(filter 1,$(USE_NVBOARD)),$(NVBOARD_ARCHIVE)) \
		$(VERILATOR_CFLAGS) $(VERILATOR_LDFLAGS)
	@echo "--- Running Simulation ---"
	$(BIN) IMG=$(IMG) BUILD=$(BUILD_DIR)
	
rt: verilog
	$(MAKE) -C /home/hy5un/ysyx-workbench/rt-thread-am/bsp/abstract-machine run \
	  ARCH=$(if $(filter 1,$(USE_YSYXSOC)),riscv32e-ysyxsoc,riscv32e-npc)


CORENAME = ysyx_26010036


syn:
	$(MAKE) USE_YSYXSOC=1 ENABLE_DPIC=0 clean verilog
	$(MAKE) do_syn
do_syn:
	$(call git_commit, "run yosys synthesis")
	@mkdir -p $(YOSYS_STA_DIR)/result
	@if $(MAKE) -C $(YOSYS_STA_DIR) syn \
		DESIGN=$(CORENAME) \
		RTL_FILES="$(CORE_VSRCS)" \
		CLK_FREQ_MHZ=$(CLK_FREQ_MHZ) \
		CLK_PORT_NAME=$(CLK_PORT_NAME) \
		SDC_FILE=$(abspath $(SDC_FILE)); then \
		echo "=> Synthesis Success. Restoring RTL files..."; \
	else \
		echo "=> Synthesis Failed! Restoring RTL files..."; \
		exit 1; \
	fi
sta:
	$(MAKE) USE_YSYXSOC=1 do_sta
do_sta:
	$(call git_commit, "run yosys sta")
	@mkdir -p $(YOSYS_STA_DIR)/result
	$(MAKE) -C $(YOSYS_STA_DIR) sta \
		DESIGN=$(CORENAME) \
		RTL_FILES="$(VSRCS)" \
		CLK_FREQ_MHZ=$(CLK_FREQ_MHZ) \
		CLK_PORT_NAME=$(CLK_PORT_NAME) \
		SDC_FILE=$(abspath $(SDC_FILE))

perf:
	$(MAKE) syn sta
	$(MAKE) USE_YSYXSOC=1 verilog
	$(MAKE) -C ../am-kernels/benchmarks/microbench run ARCH=riscv32e-ysyxsoc

mi: verilog
	$(MAKE) -C ../am-kernels/benchmarks/microbench run ARCH=riscv32e-ysyxsoc

.PHONY: sim verilog test vsim rt syn sta perf