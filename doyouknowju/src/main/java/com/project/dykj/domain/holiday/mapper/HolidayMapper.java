package com.project.dykj.domain.holiday.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

@Mapper
@Repository
public interface HolidayMapper {
    
    public void deleteHolidays(String yearAndMonth);

    public void insertHolidays(List<String> holidays);

    public int getIsHoliday(String day);
}
