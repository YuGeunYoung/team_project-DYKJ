package com.project.dykj.domain.ranking.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.dykj.domain.ranking.dto.req.PageReq;
import com.project.dykj.domain.ranking.dto.res.RankingRes;
import com.project.dykj.domain.ranking.mapper.RankingMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class RankingService {
    
    private final RankingMapper rankingMapper;

    public List<RankingRes> getWeeklyRanking(PageReq pageReq) {
        List<RankingRes> rankingResList = rankingMapper.selectWeeklyRanking(pageReq);
        return rankingResList;
    }

    public List<RankingRes> getMonthlyRanking(PageReq pageReq) {
        List<RankingRes> rankingResList = rankingMapper.selectMonthlyRanking(pageReq);
        return rankingResList;
    }

    public List<RankingRes> getYearlyRanking(PageReq pageReq) {
        List<RankingRes> rankingResList = rankingMapper.selectYearlyRanking(pageReq);
        return rankingResList;
    }

    public List<RankingRes> getAllRanking(PageReq pageReq) {
        List<RankingRes> rankingResList = rankingMapper.selectAllRanking(pageReq);
        return rankingResList;
    }
}
