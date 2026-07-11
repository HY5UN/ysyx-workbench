# ==============================================================================
# iverilog 四值与网表仿真规则 (由主 Makefile 包含)
# ==============================================================================

# 基础路径配置（相对于主 Makefile 所在根目录）
IVERILOG_SIM_DIR = $(BUILD_DIR)/iverilog_sim
IVERILOG_TOP     = $(abspath ./resource/iverilog/sim_top.v)

# 脚本与中间生成物路径
WRAPPER_SCRIPT   = $(abspath ./tools/gen_wrapper.py)
NETLIST_WRAPPER  = $(IVERILOG_SIM_DIR)/$(CORENAME)_wrapper.v
MODIFIED_NETLIST = $(IVERILOG_SIM_DIR)/$(CORENAME)_netlist_copy.v
IVERILOG_STAMP   = $(IVERILOG_SIM_DIR)/.generate.stamp
IVERILOG_OUT     = $(IVERILOG_SIM_DIR)/sim.out
IVERILOG_NET_OUT = $(IVERILOG_SIM_DIR)/sim_netlist.out
IVERILOG_HEX     = $(IVERILOG_SIM_DIR)/image.hex

# ------------------------------------------------------------------------------
# 网表仿真缺省路径配置（支持通过 make 传参覆盖，如 NETLIST=yyy CELLS=zzz）
# ------------------------------------------------------------------------------
NETLIST ?= $(abspath ./yosys-sta/result/$(CORENAME)-$(CLK_FREQ_MHZ)MHz/$(CORENAME).netlist.v)
CELLS   ?= $(abspath ./yosys-sta/pdk/nangate45/sim/cells.v)

# ------------------------------------------------------------------------------
# 1. 独立生成 iverilog 专用的 Verilog
# ------------------------------------------------------------------------------
$(IVERILOG_STAMP): $(SCALA_SRCS)
	@echo "--- Generating standalone Verilog for iverilog ---"
	@mkdir -p $(IVERILOG_SIM_DIR)
	USE_YSYXSOC=0 ENABLE_DPIC=0 ./mill -i runMain ElaborateFull --target-dir $(IVERILOG_SIM_DIR)
	@touch $@

# ------------------------------------------------------------------------------
# 2. 自动生成网表转接桥并拷贝/修改网表
# ------------------------------------------------------------------------------
$(NETLIST_WRAPPER): $(IVERILOG_STAMP) $(NETLIST) $(WRAPPER_SCRIPT)
	@echo "--- Auto-generating Wrapper and Copying Netlist ---"
	@CORE_RTL=`find $(IVERILOG_SIM_DIR) -maxdepth 1 -name "$(CORENAME).v" -o -name "$(CORENAME).sv" | head -n 1`; \
	if [ -z "$$CORE_RTL" ]; then \
		echo "Error: Cannot find $(CORENAME) RTL file in $(IVERILOG_SIM_DIR)"; exit 1; \
	fi; \
	python3 $(WRAPPER_SCRIPT) $$CORE_RTL $(NETLIST) $(MODIFIED_NETLIST) $@ $(CORENAME)

# ------------------------------------------------------------------------------
# 3. 将 BIN 转为 HEX，依赖于输入的 IMG 文件
# ------------------------------------------------------------------------------
$(IVERILOG_HEX): $(IMG)
	@if [ -z "$(IMG)" ]; then echo "Error: IMG is not defined. Use 'make sim-iverilog IMG=xxx.bin'"; exit 1; fi
	@mkdir -p $(IVERILOG_SIM_DIR)
	@echo "--- Converting BIN to HEX for readmemh ---"
	@od -v -t x4 -An $(IMG) | awk '{for(i=1;i<=NF;i++) print $$i}' > $@

# ------------------------------------------------------------------------------
# 4. 编译出 sim.out (普通 RTL 仿真)
# ------------------------------------------------------------------------------
$(IVERILOG_OUT): $(IVERILOG_TOP) $(IVERILOG_STAMP)
	@echo "--- Compiling RTL with iverilog ---"
	@IVERILOG_RTL_SRCS=`find $(IVERILOG_SIM_DIR) -maxdepth 1 -name "*.v" -o -name "*.sv"`; \
	iverilog -g2012 -o $@ $(IVERILOG_TOP) $$IVERILOG_RTL_SRCS

# ------------------------------------------------------------------------------
# 5. 编译出 sim_netlist.out (网表仿真)
# ------------------------------------------------------------------------------
$(IVERILOG_NET_OUT): $(IVERILOG_TOP) $(IVERILOG_STAMP) $(NETLIST_WRAPPER) $(CELLS)
	@echo "--- Compiling Netlist with iverilog ---"
	@IVERILOG_NET_SRCS=`find $(IVERILOG_SIM_DIR) -maxdepth 1 \( -name "*.v" -o -name "*.sv" \) ! -name "$(CORENAME).v" ! -name "$(CORENAME).sv"`; \
	iverilog -g2012 -o $@ $(IVERILOG_TOP) $$IVERILOG_NET_SRCS $(CELLS)

# ------------------------------------------------------------------------------
# 6. RTL 仿真顶层伪目标
# ------------------------------------------------------------------------------
sim-iverilog: $(IVERILOG_OUT) $(IVERILOG_HEX)
	$(call git_commit, "sim RTL with iverilog")
	@echo "--- Running iverilog simulation (RTL) ---"
	cd $(IVERILOG_SIM_DIR) && vvp $(notdir $(IVERILOG_OUT)) -fst

# ------------------------------------------------------------------------------
# 7. 网表仿真伪目标
# ------------------------------------------------------------------------------
sim-iverilog-netlist: $(IVERILOG_NET_OUT) $(IVERILOG_HEX)
	$(call git_commit, "sim Netlist with iverilog")
	@echo "--- Running iverilog simulation (Netlist) ---"
	cd $(IVERILOG_SIM_DIR) && vvp $(notdir $(IVERILOG_NET_OUT)) -fst