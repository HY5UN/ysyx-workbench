ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; }

MEMORY {
  mrom : ORIGIN = 0x20000000, LENGTH = 4K
  sram : ORIGIN = 0x0f000000, LENGTH = 8K
}


SECTIONS {
  . = ORIGIN(mrom);
  .text : {
    *(entry)
    *(.text*)
  } > mrom AT> mrom : text

  .rodata : {
    *(.rodata*)
    *(.srodata*)        /* 小只读数据也要收 */
  } > mrom AT> mrom : text

  .data : {
    _data_start = .;
    *(.data*)
    *(.sdata*)          /* 关键:收集小数据段 */
    . = ALIGN(4);
    _data_end = .;
  } > sram AT> mrom : data
  _data_lma = LOADADDR(.data);

  .bss (NOLOAD) : {     /* 关键:NOLOAD,不进镜像 */
    _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
    *(COMMON)
    . = ALIGN(4);
    _bss_end = .;
  } > sram

  _heap_start = ALIGN(4);
  _heap_end = _heap_start + 0x1800;
  . = ORIGIN(sram) + LENGTH(sram);
  _stack_pointer = .;
  end = .;
  _end = .;
}