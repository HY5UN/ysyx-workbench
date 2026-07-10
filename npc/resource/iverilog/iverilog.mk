# ==============================================================================
# iverilog 四值与网表仿真规则 (由主 Makefile 包含)
# ==============================================================================

# 基础路径配置（相对于主 Makefile 所在根目录）
IVERILOG_SIM_DIR = $(BUILD_DIR)/iverilog_sim
IVERILOG_TOP     = $(abspath ./resource/iverilog/sim_top.v)

# 定义具体的中间/生成物路径
IVERILOG_SV  = $(IVERILOG_SIM_DIR)/$(TOPNAME).sv
IVERILOG_OUT = $(IVERILOG_SIM_DIR)/sim.out
IVERILOG_HEX = $(IVERILOG_SIM_DIR)/image.hex

# ------------------------------------------------------------------------------
# 1. 独立生成 iverilog 专用的 Verilog
# ------------------------------------------------------------------------------
$(IVERILOG_SV):
	@echo "--- Generating standalone Verilog for iverilog ---"
	@mkdir -p $(IVERILOG_SIM_DIR)
	USE_YSYXSOC=0 ENABLE_DPIC=0 ./mill -i runMain ElaborateFull --target-dir $(IVERILOG_SIM_DIR)
	@sh -c 'cat $(IVERILOG_SIM_DIR)/*.v $(IVERILOG_SIM_DIR)/*.sv > $@.tmp 2>/dev/null || true'
	@sh -c 'rm -f $(IVERILOG_SIM_DIR)/*.v $(IVERILOG_SIM_DIR)/*.sv'
	@mv $@.tmp $@

# ------------------------------------------------------------------------------
# 2. 将 BIN 转为 HEX，依赖于输入的 IMG 文件
# ------------------------------------------------------------------------------
$(IVERILOG_HEX): $(IMG)
	@if [ -z "$(IMG)" ]; then echo "Error: IMG is not defined. Use 'make sim-iverilog IMG=xxx.bin'"; exit 1; fi
	@mkdir -p $(IVERILOG_SIM_DIR)
	@echo "--- Converting BIN to HEX for readmemh ---"
	@od -v -t x4 -An $(IMG) | awk '{for(i=1;i<=NF;i++) print $$i}' > $@

# ------------------------------------------------------------------------------
# 3. 编译出 sim.out
# ------------------------------------------------------------------------------
$(IVERILOG_OUT): $(IVERILOG_TOP) $(IVERILOG_SV) $(IVERILOG_HEX)
	@echo "--- Compiling with iverilog ---"
	iverilog -g2012 -o $@ $(IVERILOG_TOP) $(IVERILOG_SV)

# ------------------------------------------------------------------------------
# 4. RTL 仿真顶层伪目标
# ------------------------------------------------------------------------------
sim-iverilog: $(IVERILOG_OUT)
	$(call git_commit, "sim RTL with iverilog")
	@echo "--- Running iverilog simulation (FST Waveform) ---"
	cd $(IVERILOG_SIM_DIR) && vvp $(notdir $(IVERILOG_OUT)) -fst

# ------------------------------------------------------------------------------
# 5. 网表仿真伪目标
# ------------------------------------------------------------------------------
sim-iverilog-netlist: $(IVERILOG_HEX)
	$(call git_commit, "sim netlist with iverilog")
	@if [ -z "$(NETLIST)" ] || [ -z "$(CELLS)" ]; then \
		echo "Error: Missing args. Use 'make sim-iverilog-netlist IMG=xxx.bin NETLIST=yyy CELLS=zzz'"; exit 1; \
	fi
	@echo "--- Compiling Netlist with iverilog ---"
	iverilog -g2012 -o $(IVERILOG_SIM_DIR)/sim_netlist.out $(IVERILOG_TOP) $(NETLIST) $(CELLS)
	@echo "--- Running iverilog Netlist simulation (FST Waveform) ---"
	cd $(IVERILOG_SIM_DIR) && vvp -fst sim_netlist.out

# 声明伪目标，防止与同名文件冲突
.PHONY: sim-iverilog sim-iverilog-netlist