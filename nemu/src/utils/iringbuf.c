#include "utils.h"

IringbufEntry iringbuf[IRINGBUF_SIZE];
int iringbuf_index;


void iringbuf_push(word_t pc, word_t inst, int ilen)
{
  iringbuf[iringbuf_index].pc = pc;
  iringbuf[iringbuf_index].inst = inst;
  iringbuf[iringbuf_index].ilen = ilen;
  iringbuf[iringbuf_index].valid = true;

  iringbuf_index = (iringbuf_index + 1) % IRINGBUF_SIZE;
}


void iringbuf_display()
{
  int last_index = (iringbuf_index - 1 + IRINGBUF_SIZE) % IRINGBUF_SIZE;
  int old_index = iringbuf_index;
  int i = old_index;

  for (int cnt=0;cnt<IRINGBUF_SIZE;cnt++)
  {
    if (!iringbuf[i].valid)
    {
      i = (i + 1) % IRINGBUF_SIZE;
      continue;
    }

    if (i == last_index)
      printf("--> ");

    char asmstr[128];

    void disassemble(char *str, int size, uint64_t pc, uint8_t *code, int nbyte);
    disassemble(asmstr, sizeof(asmstr) - 1, iringbuf[i].pc, (uint8_t *)&iringbuf[i].inst, iringbuf[i].ilen);

    printf(FMT_WORD ": " FMT_WORD " %s\n", iringbuf[i].pc, iringbuf[i].inst, asmstr);
    if (i == last_index)
      return;

    i = (i + 1) % IRINGBUF_SIZE;
  }

  printf("No valid instruction in the ring buffer.\n");

  return;
}
