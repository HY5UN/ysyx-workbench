# ==============================================================================
# 全局配置
# ==============================================================================
TOPNAME = top
PRJ = playground
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)

# 告诉 make 不要删除中间文件
.SECONDARY:

# 引入 ysyx-workbench 的通用 Makefile (为了 git_commit 等功能)
-include ../Makefile

# ==============================================================================
# 1. Chisel 构建规则 (Mill)
# ==============================================================================

# 默认目标
default: sim

# 调用 Mill 生成 Verilog
# 注意：这会把 .v 文件输出到 $(BUILD_DIR)
verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	mill -i $(PRJ).runMain Elaborate --target-dir $(BUILD_DIR)

help:
	mill -i $(PRJ).runMain Elaborate --help

reformat:
	mill -i __.reformat

checkformat:
	mill -i __.checkFormat

clean:
	-rm -rf $(BUILD_DIR)

.PHONY: verilog help reformat checkformat clean

# ==============================================================================
# 2. C++ / Verilator 仿真源文件配置
# ==============================================================================

# VSRCS 现在的来源是 build 目录 (由 Chisel 生成)
# 注意：我们需要先运行 verilog 目标才能找到这些文件，所以这里使用 wildcard 是为了稳健性
# 实际编译时依赖关系会确保文件存在
VSRCS = $(shell find $(abspath $(BUILD_DIR)) -name "*.v")
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

# 指向 NVBoard 的 mk 文件
include $(NVBOARD_HOME)/scripts/nvboard.mk

# NVBoard 自动绑定逻辑
SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
CSRCS += $(SRC_AUTO_BIND)

# 生成绑定代码的规则
$(SRC_AUTO_BIND): $(NXDC_FILES)
	@mkdir -p $(BUILD_DIR)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

# NVBoard 编译参数
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

# 这里的逻辑是：
# 1. 先执行 verilog (调用 Mill 生成 RTL)
# 2. 确保 NVBoard 的 auto_bind 生成 (如果启用)
# 3. 使用 Verilator 编译 build/ 下的 .v 和 csrc/ 下的 .cpp
# 4. 运行生成的二进制文件

sim: verilog $(SRC_AUTO_BIND)
	$(call git_commit, "sim RTL")
	@echo "--- Building $(TOPNAME) with Verilator ---"
	@mkdir -p $(OBJ_DIR)
	
	@# 注意：这里重新执行 find 以获取刚刚生成的 .v 文件
	$(eval VSRCS := $(shell find $(abspath $(BUILD_DIR)) -name "*.v"))

	verilator $(VERILATOR_FLAGS) $(VERILATOR_CFLAGS) $(VERILATOR_LDFLAGS) \
		$(VSRCS) $(CSRCS)

	@echo "--- Running Simulation ---"
	$(BIN)

.PHONY: sim