# ==============================================================================
# 全局配置
# ==============================================================================
TOPNAME = ysyxSoCFull
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)
YSYXSOC_DIR = $(abspath ../ysyxSoC)

YOSYS_STA_DIR ?= $(abspath ./yosys-sta)
CLK_FREQ_MHZ ?= 500
CLK_PORT_NAME ?= clock
SDC_FILE ?= $(YOSYS_STA_DIR)/scripts/default.sdc

# ==============================================================================
# 切换流程：1、改变开关 2、修改Elaborate里top名 3、用新配置编译nemu 4、改变ifu初始pc
# ==============================================================================
USE_YSYXSOC ?= 0

MOCKSOC_DIR = ./src/main/scala/bus/mockSoc

.SECONDARY:
-include ../Makefile

# ==============================================================================
# Chisel 构建 (Mill)
# ==============================================================================

ifeq ($(USE_YSYXSOC),1)

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	@echo "--- [USE_YSYXSOC=1] Disabling mockSoc Scala sources ---"
	@find $(MOCKSOC_DIR) -maxdepth 1 -name "*.scala" \
	  -exec sh -c 'mv "$$1" "$$1.disabled"' _ {} \;
	./mill -i runMain Elaborate --target-dir $(BUILD_DIR)
	$(MAKE) strip_sv_loc

else

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	@echo "--- [USE_YSYXSOC=0] Restoring mockSoc Scala sources ---"
	@find $(MOCKSOC_DIR) -maxdepth 1 -name "*.scala.disabled" \
	  -exec sh -c 'mv "$$1" "$${1%.disabled}"' _ {} \;
	./mill -i runMain Elaborate --target-dir $(BUILD_DIR)
	$(MAKE) strip_sv_loc

endif

test:
	./mill -i test

strip_sv_loc:
	@echo "--- Stripping locator comments from $(BUILD_DIR)/*.sv ---"
	@set -e; \
	if [ -d "$(BUILD_DIR)" ]; then \
	  find "$(BUILD_DIR)" -type f -name "*.sv" -print0 | \
	    xargs -0 -r sed -i -E \
	      -e '/^[[:space:]]*\/\/[[:space:]]*@/d' \
	      -e 's|[[:space:]]*\/\/[[:space:]]*@.*$$||'; \
	fi

help:
	./mill -i runMain Elaborate --help

reformat:
	./mill -i __.reformat

checkformat:
	./mill -i __.checkFormat

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: verilog help reformat checkformat clean

# ==============================================================================
# 源文件
# ==============================================================================

# 从 BUILD_DIR 通配结果中剔除 ysyxSoCFull.v/.sv，避免两份同时混入
_BUILD_VSRCS = $(filter-out \
  %/ysyxSoCFull.v %/ysyxSoCFull.sv, \
  $(wildcard $(abspath $(BUILD_DIR))/*.v) \
  $(wildcard $(abspath $(BUILD_DIR))/*.sv))

_VSRCS_COMMON = \
  $(_BUILD_VSRCS) \
  $(shell find $(abspath ./src/main) \( -name "*.v" -o -name "*.sv" \)) \
  $(shell find $(YSYXSOC_DIR)/perip -name "*.v")

ifeq ($(USE_YSYXSOC),1)
VSRCS = $(_VSRCS_COMMON) $(YSYXSOC_DIR)/build/ysyxSoCFull.v
else
VSRCS = $(_VSRCS_COMMON) $(abspath $(BUILD_DIR)/ysyxSoCFull.sv)
endif

CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -o -name "*.cc" -o -name "*.cpp")

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

VERILATOR_CFLAGS  += $(addprefix -CFLAGS , $(CXXFLAGS)) \
                     $(addprefix -CFLAGS , -I$(abspath ./csrc)) \
                     -CFLAGS -DUSE_YSYXSOC=$(USE_YSYXSOC)

LDFLAGS           += -lreadline
VERILATOR_LDFLAGS += $(addprefix -LDFLAGS , $(LDFLAGS))

# ==============================================================================
# 仿真规则
# ==============================================================================

default: sim

vsim: verilog sim

sim:
	$(call git_commit, "sim RTL")
	@echo "--- Building $(TOPNAME) with Verilator ---"
	@mkdir -p $(OBJ_DIR)
	@if [ -z "$(VSRCS)" ]; then \
		echo "Error: No .v/.sv files found. Run 'make verilog' first."; \
		exit 1; \
	fi
	verilator $(VERILATOR_FLAGS) $(VERILATOR_CFLAGS) $(VERILATOR_LDFLAGS) \
		$(VSRCS) $(CSRCS)
	@echo "--- Running Simulation ---"
	$(BIN) IMG=$(IMG) BUILD=$(BUILD_DIR) 

rt: verilog
	$(MAKE) -C /home/hy5un/Templates/rt-thread-am/bsp/abstract-machine run ARCH=riscv32e-npc

syn:
	$(call git_commit, "run yosys synthesis")
	@mkdir -p $(YOSYS_STA_DIR)/result
	$(MAKE) -C $(YOSYS_STA_DIR) syn \
		DESIGN=$(TOPNAME) \
		RTL_FILES="$(VSRCS)" \
		CLK_FREQ_MHZ=$(CLK_FREQ_MHZ) \
		CLK_PORT_NAME=$(CLK_PORT_NAME) \
		SDC_FILE=$(abspath $(SDC_FILE))

sta:
	$(call git_commit, "run yosys sta")
	@mkdir -p $(YOSYS_STA_DIR)/result
	$(MAKE) -C $(YOSYS_STA_DIR) sta \
		DESIGN=$(TOPNAME) \
		RTL_FILES="$(VSRCS)" \
		CLK_FREQ_MHZ=$(CLK_FREQ_MHZ) \
		CLK_PORT_NAME=$(CLK_PORT_NAME) \
		SDC_FILE=$(abspath $(SDC_FILE))

.PHONY: sim verilog test vsim rt syn sta strip_sv_loc