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
  } > mrom AT> mrom

  .data : {
    _data_start = .;
    *(.data*)
  } > sram AT> mrom : data

  _data_lma = LOADADDR(.data);

  edata = .;
  
  .bss : {
	  _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
  } > sram
  _heap_start = .;
  . = ORIGIN(sram) + LENGTH(sram);
  _stack_pointer = .;
  _heap_end = .;
  end = .;
  _end = .;
}
