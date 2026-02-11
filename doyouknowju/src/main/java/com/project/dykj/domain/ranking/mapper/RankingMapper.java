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
    public int getCurrentSeasonNo(@Param("seasonPeriod") String seasonPeriod);
    public int increaseCurrentSeasonNo(@Param("seasonPeriod") String seasonPeriod);
    public List<RankingRes> selectSeasonRanking(@Param("seasonPeriod") String seasonPeriod, @Param("seasonNo") int seasonNo, @Param("pageReq") PageReq pageReq);
    public List<RankingRes> selectSeasonTop100Ranking(@Param("seasonPeriod") String seasonPeriod, @Param("seasonNo") int seasonNo);
    public List<RankingRes> selectWeeklyRanking(PageReq pageReq);
    public List<RankingRes> selectMonthlyRanking(PageReq pageReq);
    public List<RankingRes> selectYearlyRanking(PageReq pageReq);
    public List<AllRankingRes> selectAllRanking(PageReq pageReq);

    public void updateSeasonRanking();
    public void insertCurrentSeasonRanking(@Param("seasonPeriod") String seasonPeriod);
    public void insertNewSeasonRanking(@Param("seasonPeriod") String seasonPeriod);
    public int selectSeasonRankingCount(@Param("seasonPeriod") String seasonPeriod);
}
