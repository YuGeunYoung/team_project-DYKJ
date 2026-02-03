package com.project.dykj.domain.ranking.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.project.dykj.domain.ranking.dto.req.PageReq;
import com.project.dykj.domain.ranking.dto.res.AllRankingRes;
import com.project.dykj.domain.ranking.dto.res.RankingRes;

@Mapper
@Repository
public interface RankingMapper {
    int getCurrentWeeklySeasonNo();
    int getCurrentMonthlySeasonNo();
    int getCurrentYearlySeasonNo();
    int increaseCurrentWeeklySeasonNo();
    int increaseCurrentMonthlySeasonNo();
    int increaseCurrentYearlySeasonNo();
    List<RankingRes> selectSeasonRanking(@Param("season") String season, @Param("seasonNo") int seasonNo, @Param("pageReq") PageReq pageReq);
    List<RankingRes> selectWeeklyRanking(PageReq pageReq);
    List<RankingRes> selectMonthlyRanking(PageReq pageReq);
    List<RankingRes> selectYearlyRanking(PageReq pageReq);
    List<AllRankingRes> selectAllRanking(PageReq pageReq);
}
