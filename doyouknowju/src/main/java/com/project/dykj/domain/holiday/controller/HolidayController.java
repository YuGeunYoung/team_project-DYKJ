package com.project.dykj.domain.holiday.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.holiday.service.HolidayService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/holiday")
public class HolidayController {

    private final HolidayService holidayService;
    
    @GetMapping("/check")
    public int getIsHoliday() {
        return holidayService.getIsHoliday();
    }
}
