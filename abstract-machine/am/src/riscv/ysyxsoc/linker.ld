ENTRY(_start)
PHDRS { init PT_LOAD; text PT_LOAD; data PT_LOAD; }

MEMORY {
  sram : ORIGIN = 0x0f000000, LENGTH = 8k
  psram : ORIGIN = 0x80000000, LENGTH = 4M
  flash : ORIGIN = 0x30000000, LENGTH = 16M
}


SECTIONS {
  . = ORIGIN(flash);
  .init : {
    *(entry)
    *(.init) 

  } > flash : init


  .text : {
    _text_start = .;
    *(.text*)
  } > sram AT> flash : text
  _text_end = .;
  _text_lma = LOADADDR(.text);

  .rodata : {
    _rodata_start = .;
    *(.rodata*)
  } > sram AT> flash
  _rodata_end = .;
  _rodata_lma = LOADADDR(.rodata);

  .data : {
    _data_start = .;
    *(.data*)
    . = ALIGN(4); 
  } > sram AT> flash : data

  _data_lma = LOADADDR(.data);

  _data_end = .;
  
  .bss : {
	  _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
  } > sram
  _bss_end = .;
  _heap_start = .;
  

  . = ORIGIN(sram) + LENGTH(sram);
  _heap_end = .;
  _stack_pointer = .;
  end = .;
  _end = .;
}
