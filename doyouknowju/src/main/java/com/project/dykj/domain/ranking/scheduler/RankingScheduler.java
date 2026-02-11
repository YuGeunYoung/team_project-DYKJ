package com.project.dykj.domain.ranking.scheduler;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.cglib.core.Local;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.project.dykj.domain.member.mapper.MemberMapper;
import com.project.dykj.domain.ranking.service.RankingService;
import com.project.dykj.domain.stock.service.StockInfoService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@RequiredArgsConstructor
public class RankingScheduler {

    private final MemberMapper memberMapper;
    private final RankingService rankingService;
    private final StockInfoService stockInfoService;
    
    // 매일 자정 주간, 월간, 연간 랭킹을 업데이트한다.
    // 여기서 수행하는 업데이트는 각 회원의 보유 자산 총합으로 현재 포인트를 업데이트시키는 것이다.
    // 현재 자산을 계산하기 위해, 보유 주식의 현재가와 보유 현금을 합산한다.
    // 보유 주식의 현재가는 주식 현재가 테이블에서 가져온다.
    // 보유 현금은 회원 테이블에서 가져온다.
    // 랭킹 테이블에 업데이트한다.
    
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Seoul")
    public void updateRanking() {
        stockInfoService.syncStockInfo();
        rankingService.updateRanking();
        rankingService.insertCurrentSeasonRanking();
    }

    @Scheduled(cron = "0 0 1 * * *", zone = "Asia/Seoul")
    public void insertNewSeasonRanking() {
        try {
            rankingService.insertNewSeasonRanking();
        } catch (Exception e) {
            log.error("===Inserting New Season Ranking Failed===");
            log.error(e.getMessage());
        }
    }
}
