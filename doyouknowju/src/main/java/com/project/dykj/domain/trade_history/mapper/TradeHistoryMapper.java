package com.project.dykj.domain.trade_history.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.project.dykj.domain.trade_history.dto.req.PageReq;
import com.project.dykj.domain.trade_history.dto.res.TradeHistoryRes;

@Mapper
@Repository
public interface TradeHistoryMapper {
    public List<TradeHistoryRes> selectTradeHistory(@Param("userId") String userId, @Param("pageReq") PageReq pageReq);
    public int selectTradeHistoryCount(@Param("userId") String userId);
}
