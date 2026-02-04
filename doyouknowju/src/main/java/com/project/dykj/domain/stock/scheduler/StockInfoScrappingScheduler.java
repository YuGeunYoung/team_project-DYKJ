package com.project.dykj.domain.stock.scheduler;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.dykj.domain.stock.service.StockInfoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockInfoScrappingScheduler {
    
    private final StockInfoService stockInfoService;

    @Scheduled(cron = "0 32 16 * * *", zone = "Asia/Seoul")
    public void syncStockInfo() {
        log.info("===Syncing Stock Info===");

        try {
            stockInfoService.syncStockInfo();
            log.info("===Syncing Stock Info Succeeded===");
        } catch (Exception e) {
            log.error("===Syncing Stock Info Failed===");
            log.error(e.getMessage());
        }
    }
}
