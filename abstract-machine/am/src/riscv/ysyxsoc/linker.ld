ENTRY(_start)
PHDRS { text PT_LOAD; data PT_LOAD; }

MEMORY {
  sram : ORIGIN = 0x0f000000, LENGTH = 8k
  psram : ORIGIN = 0x80000000, LENGTH = 4M
  flash : ORIGIN = 0x30000000, LENGTH = 16M
}


SECTIONS {
  . = ORIGIN(flash);
  .text : {
    _text_start = .;
    *(entry)
    *(.text*)
  } > sram AT> flash : text
  _text_end = .;
  _text_lma = LOADADDR(.text);

  .rodata : {
    _rodata_start = .;
    *(.rodata*)
    *(.srodata*)
  } > sram AT> flash
  _rodata_end = .;
  _rodata_lma = LOADADDR(.rodata);

  .data : {
    _data_start = .;
    *(.data*)
    *(.sdata*)
    . = ALIGN(4); /*当.data 结束于 VMA 0x0f00001e时，链接器分配vma时严格遵守Al=4，
    所以.bss的VMA自动分配为0x0f000020，而bss在文件里不需要内容，只是一个占位声明，
    所以偏移没被增长，停在了.data文件末尾0x00201e，所以差值1e与vma侧20不同，导致报错，
    添加ALIGN(4)能将.data末尾的文件偏移也对齐到0x002020，使.bss的文件偏移正确*/
  } > psram AT> flash : data

  _data_lma = LOADADDR(.data);

  _data_end = .;
  
  .bss : {
	  _bss_start = .;
    *(.bss*)
    *(.sbss*)
    *(.scommon)
  } > psram
  _bss_end = .;
  _heap_start = .;
  

  . = ORIGIN(psram) + LENGTH(psram);
  _heap_end = .;
  _stack_pointer = .;
  end = .;
  _end = .;
}
