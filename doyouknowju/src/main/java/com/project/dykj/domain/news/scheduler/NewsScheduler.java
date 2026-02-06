package com.project.dykj.domain.news.scheduler;

import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import com.project.dykj.domain.news.service.NewsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class NewsScheduler {

    private final NewsService newsService;

    // 서버부팅 직후 바로 실행
    @EventListener(ApplicationReadyEvent.class)
    public void init() {
        log.info("=== 서버 시작: 최초 뉴스 갱신 실행 ===");
        scheduleNewsRefresh();
    }

    // 10분마다 실행 (0분, 10분, 20분, 30분, 40분, 50분)
    @Scheduled(cron = "0 0 */6 * * *")
    public void scheduleNewsRefresh() {
        log.info("=== 뉴스 자동 갱신 스케줄러 시작 ===");
        try {
            newsService.refreshNews();
            log.info("=== 뉴스 자동 갱신 스케줄러 완료 ===");
        } catch (Exception e) {
            log.error("=== 뉴스 자동 갱신 스케줄러 실패 ===", e);
        }
    }
}
