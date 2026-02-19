package com.project.dykj.domain.holiday.service;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.project.dykj.domain.holiday.dto.res.HolidayRes;
import com.project.dykj.domain.holiday.mapper.HolidayMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.dataformat.xml.XmlMapper;

@Service
@RequiredArgsConstructor
@Slf4j
public class HolidayService {

    private final RestTemplate restTemplate;
    private final HolidayMapper holidayMapper;
    
    @Transactional
    public void updateHolidays() {

        LocalDateTime now = LocalDateTime.now();
        String year = String.format("%04d", now.getYear());
        String month = String.format("%02d", now.getMonthValue());

        String baseUrl = "http://apis.data.go.kr/B090041/openapi/service/SpcdeInfoService/getRestDeInfo";
        String serviceKey = "5804bce7ab0848dd3213a6f1c09808866b12191c667458304f0d6f10a900a6b4";

        URI uri = UriComponentsBuilder.fromUriString(baseUrl)
            .queryParam("serviceKey", serviceKey)
            .queryParam("solYear", year)
            .queryParam("solMonth", month)
            .build(true)
            .toUri();

        log.info("uri : {}", uri);
        

        try {            
            String xmlString = restTemplate.getForObject(uri, String.class);
            log.info("xmlString : {}", xmlString);
            
            HolidayRes holidayRes = restTemplate.getForObject(uri, HolidayRes.class);
            log.info("holidayRes : {}", holidayRes);

            if (holidayRes != null && holidayRes.getResponse() != null && holidayRes.getResponse().getBody() != null && holidayRes.getResponse().getBody().getItems() != null) {
                List<String> holidays = holidayRes.getResponse().getBody().getItems().getItem().stream()
                    .map(HolidayRes.Item::getLocdate)
                    .collect(Collectors.toList());

                holidayMapper.deleteHolidays(year + month);
                holidayMapper.insertHolidays(holidays);

                log.info("Holidays: {}", holidays);
            }

            log.info("updating holidays success");
        } catch (Exception e) {
            log.error("Error fetching holidays", e);
        }
    }


    public int getIsHoliday() {
        LocalDateTime now = LocalDateTime.now();
        String day = String.format("%04d%02d%02d", now.getYear(), now.getMonthValue(), now.getDayOfMonth());
        int result = holidayMapper.getIsHoliday(day);
        return result;
    }
}
