# 顶层模块名 (需与 vsrc 中的模块名一致)
TOPNAME = top

VSRC_DIR = vsrc
CSRC_DIR = csrc
OBJ_DIR  = obj_dir

VSRCS = $(shell find $(VSRC_DIR) -name "*.v")
CSRCS = $(shell find $(CSRC_DIR) -name "*.cpp" -or -name "*.cc")

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
	
	@echo "--- Running Simulation ---"
	./$(OBJ_DIR)/V$(TOPNAME)
	
	@echo "--- Git Tracking ---"
	$(call git_commit, "sim RTL")

clean:
	rm -rf $(OBJ_DIR) *.vcd *.fst

include ../Makefile