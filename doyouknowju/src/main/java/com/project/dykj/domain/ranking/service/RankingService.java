package com.project.dykj.domain.ranking.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.dykj.domain.ranking.dto.req.PageReq;
import com.project.dykj.domain.ranking.dto.res.AllRankingRes;
import com.project.dykj.domain.ranking.dto.res.RankingRes;
import com.project.dykj.domain.ranking.mapper.RankingMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankingService {

    private static final int GROUP_SIZE = 1;
    
    private final RankingMapper rankingMapper;

    public List<RankingRes> getPeriodRanking(String period, int page) {
        PageReq pageReq = new PageReq();
        pageReq.setPage(page);
        pageReq.setGroupSize(GROUP_SIZE);
        pageReq.setStart((page - 1) * pageReq.getGroupSize() + 1);
        pageReq.setEnd(page * pageReq.getGroupSize());
        List<RankingRes> rankingResList = rankingMapper.selectSeasonRanking(period, 1, pageReq); 
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
}
