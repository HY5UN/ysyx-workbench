# ==============================================================================
# 全局配置
# ==============================================================================
TOPNAME = top
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)

# 告诉 make 不要删除中间文件
.SECONDARY:

# 引入 ysyx-workbench 的通用 Makefile (为了 git_commit 等功能)
-include ../Makefile

# ==============================================================================
# 1. Chisel 构建规则 (Mill) - 仅在手动调用时执行
# ==============================================================================

# 手动生成 Verilog/SystemVerilog 的入口
verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	./mill -i runMain Elaborate --target-dir $(BUILD_DIR)
	$(MAKE) strip_sv_loc # 清理多余注释

test:
	./mill -i test

# 删除 build 下所有 .sv 文件中：包含 '@' 的 '//' 行内注释（例如 // @[...])
# 仅删除注释部分，不影响代码本体
strip_sv_loc:
	@echo "--- Stripping locator comments (// @...) from $(BUILD_DIR)/*.sv ---"
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
# 2. C++ / RTL 仿真源文件配置
# ==============================================================================

VSRCS = $(shell find $(abspath $(BUILD_DIR)) $(abspath ./playground/src) \( -name "*.v" -o -name "*.sv" \))
CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")

# ==============================================================================
# 3. 仿真模式配置 (NVBoard vs Pure Verilator)
# ==============================================================================

# --- 模式 A: 纯 Verilator 仿真 (无 NVBoard) ---
# 启用此块时，请确保模式 B 被注释
# -------------------------------------------
# VERILATOR_FLAGS += --trace-fst -Wall
# CPP_DEFINES += 

# --- 模式 B: 接入 NVBoard (虚拟 FPGA) ---
# 启用此块时，请确保模式 A 被注释
# -------------------------------------------
NXDC_FILES = constr/top.nxdc
INC_PATH ?=

include $(NVBOARD_HOME)/scripts/nvboard.mk

SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
CSRCS += $(SRC_AUTO_BIND)

$(SRC_AUTO_BIND): $(NXDC_FILES)
	@mkdir -p $(BUILD_DIR)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

VERILATOR_FLAGS += --x-assign fast --x-initial fast --noassert
EXT_LIBS += $(NVBOARD_ARCHIVE)
INCFLAGS = $(addprefix -I, $(INC_PATH))
CXXFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\""

# ==============================================================================
# 4. Verilator 编译标志与规则
# ==============================================================================

VERILATOR_FLAGS += --cc --exe --build \
                   --Mdir $(OBJ_DIR) \
                   -o $(abspath $(BIN)) \
                   --top-module $(TOPNAME)

# C++ 编译参数
VERILATOR_CFLAGS += $(addprefix -CFLAGS , $(CXXFLAGS)) \
                    $(addprefix -CFLAGS , -I$(abspath ./csrc)) 

# 链接参数
VERILATOR_LDFLAGS += $(addprefix -LDFLAGS , $(EXT_LIBS)) $(addprefix -LDFLAGS , $(LDFLAGS))

# ==============================================================================
# 5. 核心仿真规则 (Sim)
# ==============================================================================

default: sim

vsim: verilog sim
	@echo "--- verilog + sim workflow complete ---"

sim: $(SRC_AUTO_BIND)
	$(call git_commit, "sim RTL")
	@echo "--- Building $(TOPNAME) with Verilator ---"
	@mkdir -p $(OBJ_DIR)
	
	@# 检查是否找到了源文件，如果没有找到则报错提示用户
	@if [ -z "$(VSRCS)" ]; then \
		echo "Error: No .v or .sv files found in $(BUILD_DIR)."; \
		echo "Please run 'make verilog' first."; \
		exit 1; \
	fi

	verilator $(VERILATOR_FLAGS) $(VERILATOR_CFLAGS) $(VERILATOR_LDFLAGS) \
		$(VSRCS) $(CSRCS)

	@echo "--- Running Simulation ---"
	$(BIN)



.PHONY: sim strip_sv_loc verilog test vsim