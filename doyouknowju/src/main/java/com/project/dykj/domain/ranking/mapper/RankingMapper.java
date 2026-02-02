package com.project.dykj.domain.ranking.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.project.dykj.domain.ranking.dto.req.PageReq;
import com.project.dykj.domain.ranking.dto.res.RankingRes;

@Mapper
@Repository
public interface RankingMapper {
    List<RankingRes> selectWeeklyRanking(PageReq pageReq);
    List<RankingRes> selectMonthlyRanking(PageReq pageReq);
    List<RankingRes> selectYearlyRanking(PageReq pageReq);
    List<RankingRes> selectAllRanking(PageReq pageReq);
}
