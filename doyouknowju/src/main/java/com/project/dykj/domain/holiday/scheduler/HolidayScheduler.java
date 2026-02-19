package com.project.dykj.domain.holiday.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.dykj.domain.holiday.service.HolidayService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Component
@RequiredArgsConstructor
@Slf4j
public class HolidayScheduler {

    private final HolidayService holidayService;
    
    @Scheduled(cron = "53 48 11 * * *")
    public void updateHolidays() {
        log.info("Updating holidays");
        holidayService.updateHolidays();
    }
}
