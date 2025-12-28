TOPNAME = top
NXDC_FILES = constr/top.nxdc
INC_PATH ?=

# 这里的 wildcard 是通配符，获取当前目录下所有的 .v 和 .cpp 文件
# 如果你的文件在 vsrc 或 csrc 文件夹下，需要改为 vsrc/*.v
VSRCS = $(shell find $(abspath .) -name "*.v")
CSRCS = $(shell find $(abspath .) -name "*.c" -or -name "*.cc" -or -name "*.cpp")

BUILD_DIR = ./obj_dir
BIN = $(BUILD_DIR)/V$(TOPNAME)


all:
	@echo "Write this Makefile by your self."

sim:
	$(call git_commit, "sim RTL") # DO NOT REMOVE THIS LINE!!!
	@echo "Simulating $(TOPNAME)..."
	@verilator -cc --exe --build -j 0 --top-module $(TOPNAME) $(VSRCS) $(CSRCS) 
	@$(BIN)

include ../Makefile