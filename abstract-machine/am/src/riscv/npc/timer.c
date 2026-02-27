#include <am.h>
#include <riscv/riscv.h>
#include <npc.h>


using namespace std::chrono;


void __am_timer_init() {
}

void __am_timer_uptime(AM_TIMER_UPTIME_T *uptime) {
  uptime->us = ((uint64_t)inl(RTC_UPTIME+4))<<32 | inl(RTC_UPTIME );
}

void __am_timer_rtc(AM_TIMER_RTC_T *rtc) {

  rtc->second = inl(RTC) ;
  rtc->minute = inl(RTC + 4);
  rtc->hour   = inl(RTC + 8);
  rtc->day    = inl(RTC + 12);
  rtc->month  = inl(RTC + 16);
  rtc->year   = inl(RTC + 20);
}
