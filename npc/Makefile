# ==============================================================================
# 全局配置
# ==============================================================================
TOPNAME = ysyx_26010036
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)

YOSYS_STA_DIR ?= $(abspath ./yosys-sta)
CLK_FREQ_MHZ ?= 500
CLK_PORT_NAME ?= clock
SDC_FILE ?= $(YOSYS_STA_DIR)/scripts/default.sdc

.SECONDARY:
-include ../Makefile

# ==============================================================================
# Chisel 构建 (Mill)
# ==============================================================================

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	./mill -i runMain Elaborate --target-dir $(BUILD_DIR)
	$(MAKE) strip_sv_loc

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

VSRCS = $(wildcard $(abspath $(BUILD_DIR))/*.v) \
	$(wildcard $(abspath $(BUILD_DIR))/*.sv) \
	$(shell find $(abspath ./src/main) \( -name "*.v" -o -name "*.sv" \))

CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -o -name "*.cc" -o -name "*.cpp")

# ==============================================================================
# Verilator 编译配置
# ==============================================================================

VERILATOR_FLAGS += --cc --exe --build \
                   --Mdir $(OBJ_DIR) \
                   -o $(abspath $(BIN)) \
                   --top-module $(TOPNAME) \
                   -I$(abspath $(BUILD_DIR)/verification) \
                   --trace-fst

VERILATOR_CFLAGS  += $(addprefix -CFLAGS , $(CXXFLAGS)) \
                     $(addprefix -CFLAGS , -I$(abspath ./csrc))

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