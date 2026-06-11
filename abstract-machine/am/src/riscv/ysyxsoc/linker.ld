ENTRY(_start)

MEMORY {
  ROM : ORIGIN = 0x20000000, LENGTH = 4K
  RAM : ORIGIN = 0x0f000000, LENGTH = 8K
}

REGION_ALIAS("REGION_TEXT", ROM);
REGION_ALIAS("REGION_RODATA", ROM);
REGION_ALIAS("REGION_DATA", RAM);
REGION_ALIAS("REGION_BSS", RAM);


SECTIONS {
  . = ORIGIN(ROM);
  .text : {
    *(entry)
    *(.text*)
  } > REGION_TEXT

  .rodata : {
    *(.rodata*)
    *(.srodata*)
    rodata_end = .;
  } > REGION_RODATA

  .data : AT (rodata_end) {
    _data_start = .;
    *(.data*)
    *(.sdata*)
  } > REGION_DATA

  _data_lma = LOADADDR(.data);

  _data_end = .;
  
  .bss : {
	  _bss_start = .;
    *(.bss*)
    /* *(.sbss*)
    *(.scommon) */
  } > REGION_BSS
  _bss_end = .;
  _heap_start = .;
  _heap_end = . + 0x1800;

  . = ORIGIN(RAM) + LENGTH(RAM);
  _stack_pointer = .;
  end = .;
  _end = .;
}
