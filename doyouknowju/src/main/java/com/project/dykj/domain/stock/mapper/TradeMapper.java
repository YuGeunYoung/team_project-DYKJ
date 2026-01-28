package com.project.dykj.domain.stock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.project.dykj.domain.stock.dto.req.TradeReq;
import com.project.dykj.domain.stock.dto.res.MyTradesRes;
import com.project.dykj.domain.stock.dto.res.TradesByStockIdRes;

@Mapper
@Repository
public interface TradeMapper {

    public List<MyTradesRes> selectMyTrades(String userId);

    public List<TradesByStockIdRes> selectTradesByStockId(String stockId);

    public int insertTrade(TradeReq tradeReq);

    public int selectHoldingCount(TradeReq tradeReq);

    public int insertHolding(TradeReq tradeReq);

    public int updateHolding(TradeReq tradeReq);

    public int updateBalance(TradeReq tradeReq);
}
