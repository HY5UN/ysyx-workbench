
#include <am.h>
#include <riscv/riscv.h>
#include <npc.h>
#include <stdint.h>

#define FREQ 1436000

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime)
{
    uint32_t hi, lo, tmp;
    uint64_t cycles;

    __asm__ volatile(
        "1:\n\t"
        "csrr %[hi], mcycleh\n\t"
        "csrr %[lo], mcycle\n\t"
        "csrr %[tmp], mcycleh\n\t"
        "bne  %[hi], %[tmp], 1b"
        : [hi] "=r"(hi),
          [lo] "=r"(lo),
          [tmp] "=r"(tmp)
        :
        : "memory");
    cycles = ((uint64_t)hi << 32) | lo;

    uptime->us = (cycles * 1000000ULL) / FREQ;
}
void __am_timer_init()
{
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc)
{

    rtc->second = inl(RTC);
    rtc->minute = inl(RTC + 4);
    rtc->hour = inl(RTC + 8);
    rtc->day = inl(RTC + 12);
    rtc->month = inl(RTC + 16);
    rtc->year = inl(RTC + 20);
}
