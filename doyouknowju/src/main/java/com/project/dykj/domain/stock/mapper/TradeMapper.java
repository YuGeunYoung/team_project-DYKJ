package com.project.dykj.domain.stock.mapper;

import java.util.List;
import java.util.Optional;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.project.dykj.domain.stock.dto.req.TradeReq;
import com.project.dykj.domain.stock.dto.res.MyTradesRes;
import com.project.dykj.domain.stock.dto.res.TradesByStockIdRes;
import com.project.dykj.domain.stock.vo.Holding;

@Mapper
@Repository
public interface TradeMapper {

    public List<MyTradesRes> selectMyTrades(String userId);

    public List<TradesByStockIdRes> selectTradesByStockId(String stockId);

    public int insertTrade(TradeReq tradeReq);

    public int selectHoldingCount(TradeReq tradeReq);

    public Optional<Holding> selectHoldingByUserIdAndStockId(TradeReq tradeReq);

    public int insertHolding(TradeReq tradeReq); // HOLDINGS 테이블에 (아이디, 종목코드) 쌍 추가

    public int addHolding(TradeReq tradeReq); // HOLDINGS 테이블에서 (아이디, 종목코드) 쌍의 totalCount와 totalPrice 업데이트

    public int deleteHolding(TradeReq tradeReq); // HOLDINGS 테이블에서 (아이디, 종목코드) 쌍 삭제

    public int reduceHolding(TradeReq tradeReq); // HOLDINGS 테이블에서 (아이디, 종목코드) 쌍의 totalCount와 totalPrice 업데이트

    public int updateBalance(TradeReq tradeReq); // MEMBERS 테이블에서 userId의 points 업데이트
}
