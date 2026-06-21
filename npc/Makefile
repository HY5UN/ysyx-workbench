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
USE_YSYXSOC ?= 1

# NVBoard开关：1为接入虚拟FPGA开发板(LED/数码管/拨码开关/UART/键盘/VGA)，0为普通仿真
USE_NVBOARD ?= 0

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

else

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	@echo "--- [USE_YSYXSOC=0] Restoring mockSoc Scala sources ---"
	@find $(MOCKSOC_DIR) -maxdepth 1 -name "*.scala.disabled" \
	  -exec sh -c 'mv "$$1" "$${1%.disabled}"' _ {} \;
	./mill -i runMain Elaborate --target-dir $(BUILD_DIR)

endif

test:
	./mill -i test



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
# NVBoard 接入
# ==============================================================================
ifeq ($(USE_NVBOARD),1)

ifeq ($(NVBOARD_HOME),)
$(error NVBOARD_HOME未设置，请先 export NVBOARD_HOME=<nvboard项目路径>)
endif

# 引脚约束文件，需自行编写（参考nvboard/board下的引脚说明），格式见nvboard README
NXDC_FILES ?= $(abspath ./constr/$(TOPNAME).nxdc)

# 由约束文件自动生成的引脚绑定代码
SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	@mkdir -p $(dir $@)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

include $(NVBOARD_HOME)/scripts/nvboard.mk

endif

# ==============================================================================
# 源文件
# ==============================================================================

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
                   --output-split 10000

# VERILATOR_FLAGS += --threads 8

VERILATOR_CFLAGS  += $(addprefix -CFLAGS , $(CXXFLAGS)) \
                     $(addprefix -CFLAGS , -I$(abspath ./csrc)) \
                     -CFLAGS -DUSE_YSYXSOC=$(USE_YSYXSOC) \
                     -CFLAGS -DUSE_NVBOARD=$(USE_NVBOARD)

VERILATOR_CFLAGS += $(addprefix -CFLAGS -I, $(INC_PATH))

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
	verilator $(VERILATOR_FLAGS) $(VERILATOR_CFLAGS) $(VERILATOR_LDFLAGS) \
		$(VSRCS) $(CSRCS) $(if $(filter 1,$(USE_NVBOARD)),$(NVBOARD_ARCHIVE))
	@echo "--- Running Simulation ---"
	$(BIN) IMG=$(IMG) BUILD=$(BUILD_DIR)

rt: verilog
	$(MAKE) -C /home/hy5un/ysyx-workbench/rt-thread-am/bsp/abstract-machine run \
	  ARCH=$(if $(filter 1,$(USE_YSYXSOC)),riscv32e-ysyxsoc,riscv32e-npc)
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

.PHONY: sim verilog test vsim rt syn sta