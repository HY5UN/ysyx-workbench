# ==============================================================================
# 全局配置 & 仿真模式选择
# ==============================================================================
TOPNAME = top
BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)
SIM_MODE ?= sim
# SIM_MODE ?= nvboard

# 告诉 make 不要删除中间文件
.SECONDARY:

# 引入 ysyx-workbench 的通用 Makefile
-include ../Makefile

# ==============================================================================
# 1. Chisel 构建规则 (Mill)
# ==============================================================================

verilog:
	$(call git_commit, "generate verilog")
	mkdir -p $(BUILD_DIR)
	./mill -i runMain Elaborate --target-dir $(BUILD_DIR)
	$(MAKE) strip_sv_loc

test:
	./mill -i test

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
# 2. C++ / RTL 源文件处理 (自动处理 main 函数冲突)
# ==============================================================================

VSRCS = $(shell find $(abspath $(BUILD_DIR)) $(abspath ./src/main) \( -name "*.v" -o -name "*.sv" \))
ALL_CSRCS = $(shell find $(abspath ./csrc) -name "*.c" -or -name "*.cc" -or -name "*.cpp")

# [关键步骤]: 剔除所有的 main 文件，稍后根据模式加回来
CSRCS_NOMAIN = $(filter-out %sim-main.cpp %nvboard-main.cpp, $(ALL_CSRCS))

# ==============================================================================
# 3. 仿真模式分支处理 (NVBoard vs Pure Sim)
# ==============================================================================

ifeq ($(SIM_MODE),nvboard)
    # --- 模式: NVBoard ---
    @echo "Build Mode: NVBoard"
    
    # 指定 Main 文件
    CSRCS = $(CSRCS_NOMAIN) $(abspath ./csrc/nvboard-main.cpp)
    
    # NVBoard 配置
    NXDC_FILES = constr/top.nxdc
    INC_PATH ?=
    
    # 引入 NVBoard SDK
    include $(NVBOARD_HOME)/scripts/nvboard.mk
    
    # 生成引脚绑定代码
    SRC_AUTO_BIND = $(abspath $(BUILD_DIR)/auto_bind.cpp)
    CSRCS += $(SRC_AUTO_BIND)
    
    $(SRC_AUTO_BIND): $(NXDC_FILES)
	@mkdir -p $(BUILD_DIR)
	python3 $(NVBOARD_HOME)/scripts/auto_pin_bind.py $^ $@

    # 依赖规则：NVBoard 模式需要 auto_bind
    SIM_DEPENDENCIES = $(SRC_AUTO_BIND)

    # 编译参数
    VERILATOR_FLAGS += --x-assign fast --x-initial fast --noassert
    EXT_LIBS += $(NVBOARD_ARCHIVE)
    INCFLAGS = $(addprefix -I, $(INC_PATH))
    CXXFLAGS += $(INCFLAGS) -DTOP_NAME="\"V$(TOPNAME)\""

else
    # --- 模式: 纯仿真 (Pure Sim) ---
    @echo "Build Mode: Pure Simulation"
    
    # 指定 Main 文件
    CSRCS = $(CSRCS_NOMAIN) $(abspath ./csrc/sim-main.cpp)
    
    # 纯仿真通常不需要额外的依赖生成
    SIM_DEPENDENCIES = 
    
    # 编译参数 (开启波形追踪等)
    VERILATOR_FLAGS += --trace-fst #-Wall
    
    # 如果需要自定义 CXXFLAGS，可以在这里添加
    # CXXFLAGS += 

endif

# ==============================================================================
# 4. Verilator 编译配置
# ==============================================================================

VERILATOR_FLAGS += --cc --exe --build \
                   --Mdir $(OBJ_DIR) \
                   -o $(abspath $(BIN)) \
                   --top-module $(TOPNAME) \
                   -I$(abspath $(BUILD_DIR)/verification)

# C++ 编译参数
VERILATOR_CFLAGS += $(addprefix -CFLAGS , $(CXXFLAGS)) \
                    $(addprefix -CFLAGS , -I$(abspath ./csrc)) 

# 链接参数
VERILATOR_LDFLAGS += $(addprefix -LDFLAGS , $(EXT_LIBS)) $(addprefix -LDFLAGS , $(LDFLAGS))
LDFLAGS += -lreadline

# ==============================================================================
# 5. 核心仿真规则 (Sim)
# ==============================================================================

# ==============================================================================
# 6. Yosys-STA 规则 (NPC)
# ==============================================================================

YOSYS_STA_DIR ?= $(abspath ./yosys-sta)
NPC_DESIGN ?= top
NPC_CLK_FREQ_MHZ ?= 100
NPC_CLK_PORT_NAME ?= clock
NPC_SDC_FILE ?= $(abspath ./yosys-sta/scripts/default.sdc)
NPC_FILELIST ?= $(abspath $(BUILD_DIR))/filelist.f

default: sim

vsim: verilog sim
	@echo "--- verilog + sim workflow complete ---"

# sim 目标现在依赖于 conditional 变量 SIM_DEPENDENCIES
sim: $(SIM_DEPENDENCIES)
	$(call git_commit, "sim RTL mode:$(SIM_MODE)")
	@echo "--- Building $(TOPNAME) with Verilator [Mode: $(SIM_MODE)] ---"
	@mkdir -p $(OBJ_DIR)
	
	@# 检查源文件
	@if [ -z "$(VSRCS)" ]; then \
		echo "Error: No .v or .sv files found in $(BUILD_DIR)."; \
		echo "Please run 'make verilog' first."; \
		exit 1; \
	fi

	verilator $(VERILATOR_FLAGS) $(VERILATOR_CFLAGS) $(VERILATOR_LDFLAGS) \
		$(VSRCS) $(CSRCS)

	@echo "--- Running Simulation ---"
	$(BIN) IMG=$(IMG) BUILD=$(BUILD_DIR)

rttest:
	$(MAKE) -C /home/hy5un/Templates/rt-thread-am/bsp/abstract-machine run ARCH=riscv32e-npc

npcsyn: verilog
	@if [ ! -f "$(NPC_FILELIST)" ]; then \
		echo "Error: missing $(NPC_FILELIST)."; \
		echo "Please run 'make verilog' and ensure build/filelist.f is generated."; \
		exit 1; \
	fi
	$(MAKE) -C $(YOSYS_STA_DIR) syn \
		DESIGN=$(NPC_DESIGN) \
		CLK_FREQ_MHZ=$(NPC_CLK_FREQ_MHZ) \
		CLK_PORT_NAME=$(NPC_CLK_PORT_NAME) \
		SDC_FILE=$(NPC_SDC_FILE) \
		RTL_FILES="$$(awk '{printf "%s/%s ", "$(abspath $(BUILD_DIR))", $$0}' "$(NPC_FILELIST)")"

npcsta: verilog
	@if [ ! -f "$(NPC_FILELIST)" ]; then \
		echo "Error: missing $(NPC_FILELIST)."; \
		echo "Please run 'make verilog' and ensure build/filelist.f is generated."; \
		exit 1; \
	fi
	$(MAKE) -C $(YOSYS_STA_DIR) sta \
		DESIGN=$(NPC_DESIGN) \
		CLK_FREQ_MHZ=$(NPC_CLK_FREQ_MHZ) \
		CLK_PORT_NAME=$(NPC_CLK_PORT_NAME) \
		SDC_FILE=$(NPC_SDC_FILE) \
		RTL_FILES="$$(awk '{printf "%s/%s ", "$(abspath $(BUILD_DIR))", $$0}' "$(NPC_FILELIST)")"

.PHONY: sim strip_sv_loc verilog test vsim rttest npcsyn npcsta