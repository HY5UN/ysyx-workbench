# 顶层模块名 (需与 vsrc 中的模块名一致)
TOPNAME = top

NXDC_FILES = constr/top.nxdc #NVBoard端口连线约束文件

VSRC_DIR = vsrc
CSRC_DIR = csrc

BUILD_DIR = ./build
OBJ_DIR = $(BUILD_DIR)/obj_dir
BIN = $(BUILD_DIR)/$(TOPNAME)

VSRCS = $(shell find $(VSRC_DIR) -name "*.v")
CSRCS = $(shell find $(CSRC_DIR) -name "*.cpp" -or -name "*.cc")

# rules for NVBoard
include $(NVBOARD_HOME)/scripts/nvboard.mk

# Verilator 编译参数
# --cc: 编译为 C++
# --exe: 生成可执行文件
# --build: 自动执行 make 编译生成的 C++ 文件
# --trace-fst: 开启 FST 波形支持 (讲义推荐)
# -Wall: 开启所有警告 (讲义推荐)
VERILATOR_FLAGS = --cc --exe --build --trace-fst -Wall



all: sim

sim: $(VSRCS) $(CSRCS)
	

	@echo "--- Verilating and Building ---"
	verilator $(VERILATOR_FLAGS) \
		--top-module $(TOPNAME) \
		$(VSRCS) $(CSRCS) 

	@echo "--- Git Tracking ---"
	$(call git_commit, "sim RTL")
	
	@echo "--- Running Simulation ---"
	./$(OBJ_DIR)/V$(TOPNAME)
	
	

clean:
	rm -rf $(OBJ_DIR) *.vcd *.fst

include ../Makefile