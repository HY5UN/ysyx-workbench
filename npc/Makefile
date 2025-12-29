TOPNAME = top
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)

# 默认源文件搜索
VSRCS = $(shell find $(abspath ./vsrc) -name "*.v")
CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")

# 引入 ysyx-workbench 的通用 Makefile (为了 git_commit)
include ../Makefile


# ----------------- 模式 A: 纯 Verilator 仿真 (无 NVBoard) -----------------
# 启用此块时，请确保模式 B 被注释
# -----------------------------------------------------------------------
# VERILATOR_FLAGS += --trace-fst -Wall
# CPP_DEFINES += 

# ----------------- 模式 B: 接入 NVBoard (虚拟 FPGA) ----------------------
# 启用此块时，请确保模式 A 被注释
# -----------------------------------------------------------------------
NXDC_FILES = constr/top.nxdc
include $(NVBOARD_HOME)/scripts/nvboard.mk

# NVBoard 需要的自动绑定规则
SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
CSRCS += $(SRC_AUTO_BIND)

# 生成绑定代码的规则 (依赖 Python)
$(SRC_AUTO_BIND): $(NXDC_FILES)
	$(shell mkdir -p $(BUILD_DIR))
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

# 添加 NVBoard 的库和头文件标志
CPP_DEFINES += -DTOP_NAME="\"V$(TOPNAME)\""
VERILATOR_FLAGS += --x-assign fast --x-initial fast --noassert
EXT_LIBS += $(NVBOARD_ARCHIVE)
# -----------------------------------------------------------------------



VERILATOR_FLAGS += --cc --exe --build \
                   --Mdir $(OBJ_DIR) \
                   -o $(abspath $(BIN)) \
                   --top-module $(TOPNAME)

# 处理 C++ 编译参数 (包含 NVBoard 的 CXXFLAGS)
# addprefix 用于给 NVBoard 定义的头文件路径加 -I
VERILATOR_CFLAGS += $(CPP_DEFINES) \
                    $(addprefix -CFLAGS , $(CXXFLAGS)) \
                    $(addprefix -CFLAGS , -I$(abspath ./csrc)) \
                    $(addprefix -CFLAGS , -I$(NVBOARD_HOME)/include)
# 处理链接参数 (包含 NVBoard 的 LDFLAGS)
VERILATOR_LDFLAGS += $(addprefix -LDFLAGS , $(LDFLAGS)) $(addprefix -LDFLAGS , $(EXT_LIBS))

# ==============================================================================
# 4. 核心规则 (Rules)
# ==============================================================================

default: sim

# 伪目标：一键仿真
# 注意：NVBoard 模式下，SRC_AUTO_BIND 规则会被触发
sim: $(VSRCS) $(CSRCS) $(EXT_LIBS)
	@echo "--- Git Tracking ---"
	$(call git_commit, "sim RTL")

	@echo "--- Building $(TOPNAME) ---"
	@mkdir -p $(BUILD_DIR)
	verilator $(VERILATOR_FLAGS) $(VERILATOR_CFLAGS) $(VERILATOR_LDFLAGS) \
		$(VSRCS) $(CSRCS)

	@echo "--- Running Simulation ---"
	$(BIN)

# 清理
clean:
	rm -rf $(BUILD_DIR) *.vcd *.fst

.PHONY: default sim clean