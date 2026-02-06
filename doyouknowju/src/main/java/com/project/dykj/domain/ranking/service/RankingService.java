package com.project.dykj.domain.ranking.service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.project.dykj.domain.ranking.dto.req.PageReq;
import com.project.dykj.domain.ranking.dto.res.AllRankingRes;
import com.project.dykj.domain.ranking.dto.res.RankingRes;
import com.project.dykj.domain.ranking.mapper.RankingMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class RankingService {

    private static final int GROUP_SIZE = 1;
    
    private final RankingMapper rankingMapper;

    public List<RankingRes> getSeasonRanking(String seasonPeriod, int page) {
        PageReq pageReq = new PageReq();
        pageReq.setPage(page);
        pageReq.setGroupSize(GROUP_SIZE);
        pageReq.setStart((page - 1) * pageReq.getGroupSize() + 1);
        pageReq.setEnd(page * pageReq.getGroupSize());

        log.info("pageReq: {}", pageReq);
        log.info("seasonPeriod: {}", seasonPeriod);

        // DB에서 시즌 번호를 가져온다.
        int seasonNo = rankingMapper.getCurrentSeasonNo(seasonPeriod);
        List<RankingRes> rankingResList = rankingMapper.selectSeasonRanking(seasonPeriod, seasonNo, pageReq); 

        return rankingResList;      
    }

    public List<RankingRes> getWeeklyRanking(int page) {
        PageReq pageReq = new PageReq();
        pageReq.setPage(page);
        pageReq.setGroupSize(GROUP_SIZE);
        pageReq.setStart((page - 1) * pageReq.getGroupSize() + 1);
        pageReq.setEnd(page * pageReq.getGroupSize());
        List<RankingRes> rankingResList = rankingMapper.selectWeeklyRanking(pageReq);
        return rankingResList;
    }

    public List<RankingRes> getMonthlyRanking(int page) {
        PageReq pageReq = new PageReq();
        pageReq.setPage(page);
        pageReq.setGroupSize(GROUP_SIZE);
        pageReq.setStart((page - 1) * pageReq.getGroupSize() + 1);
        pageReq.setEnd(page * pageReq.getGroupSize());
        List<RankingRes> rankingResList = rankingMapper.selectMonthlyRanking(pageReq);
        return rankingResList;
    }

    public List<RankingRes> getYearlyRanking(int page) {
        PageReq pageReq = new PageReq();
        pageReq.setPage(page);
        pageReq.setGroupSize(GROUP_SIZE);
        pageReq.setStart((page - 1) * pageReq.getGroupSize() + 1);
        pageReq.setEnd(page * pageReq.getGroupSize());
        List<RankingRes> rankingResList = rankingMapper.selectYearlyRanking(pageReq);
        return rankingResList;
    }

    public List<AllRankingRes> getAllRanking(int page) {
        PageReq pageReq = new PageReq();
        pageReq.setPage(page);
        pageReq.setGroupSize(GROUP_SIZE);
        pageReq.setStart((page - 1) * pageReq.getGroupSize() + 1);
        pageReq.setEnd(page * pageReq.getGroupSize());
        List<AllRankingRes> rankingResList = rankingMapper.selectAllRanking(pageReq);
        return rankingResList;
    }

    @Transactional
    public void updateRanking() {
        rankingMapper.updateSeasonRanking();
    }

    @Transactional
    public void insertCurrentSeasonRanking() {
        rankingMapper.insertCurrentSeasonRanking("WEEKLY");
        rankingMapper.insertCurrentSeasonRanking("MONTHLY");
        rankingMapper.insertCurrentSeasonRanking("YEARLY");
        rankingMapper.insertCurrentSeasonRanking("ALL");
    }

    @Transactional
    public void insertNewSeasonRanking() {
        LocalDateTime now = LocalDateTime.now();

        int month = now.getMonthValue();
        int day = now.getDayOfMonth();
        DayOfWeek dayOfWeek = now.getDayOfWeek();

        if (dayOfWeek == DayOfWeek.MONDAY) {
            // 주간 시즌 시작
            // SEQ_WEEKLY_SEASON_NO을 1 상승시킨다.   
            rankingMapper.increaseCurrentSeasonNo("WEEKLY");
            rankingMapper.insertNewSeasonRanking("WEEKLY");         
        }

        if (day == 1) {
            // 월간 시즌 시작
            // SEQ_MONTHLY_SEASON_NO을 1 상승시킨다.
            rankingMapper.increaseCurrentSeasonNo("MONTHLY");
            rankingMapper.insertNewSeasonRanking("MONTHLY");
        }

        if (month == 1 && day == 1) {
            // 연간 시즌 시작
            // SEQ_YEARLY_SEASON_NO을 1 상승시킨다.
            rankingMapper.increaseCurrentSeasonNo("YEARLY");
            rankingMapper.insertNewSeasonRanking("YEARLY");
        }
    }

    public int getSeasonRankingCount(String season) {
        return rankingMapper.selectSeasonRankingCount(season);
    }
}
