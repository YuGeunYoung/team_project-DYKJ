package com.project.dykj.domain.stock.repository;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.project.dykj.domain.stock.dto.req.TradeReq;
import com.project.dykj.domain.stock.dto.res.MyTradesRes;
import com.project.dykj.domain.stock.dto.res.TradesByStockIdRes;

@Repository
public class TradeDao {


    public List<MyTradesRes> selectMyTrades(SqlSessionTemplate sqlSession, String userId) {
        return sqlSession.selectList("TradeMapper.selectMyTrades", userId);
    }

    public List<TradesByStockIdRes> selectTradesByStockId(SqlSessionTemplate sqlSession, String stockId) {
        return sqlSession.selectList("TradeMapper.selectTradesByStockId", stockId);
    }

    public int insertTrade(SqlSessionTemplate sqlSession, TradeReq buyReq) {
        return sqlSession.insert("TradeMapper.insertTrade", buyReq);
    }

    public int selectHoldingCount(SqlSessionTemplate sqlSession, TradeReq buyReq) {
        return sqlSession.selectOne("TradeMapper.selectHoldingCount", buyReq);
    }

    public int insertHolding(SqlSessionTemplate sqlSession, TradeReq buyReq) {
        return sqlSession.insert("TradeMapper.insertHolding", buyReq);
    }

    public int updateHolding(SqlSessionTemplate sqlSession, TradeReq buyReq) {
        return sqlSession.update("TradeMapper.updateHolding", buyReq);
    }

    public int updateBalance(SqlSessionTemplate sqlSession, TradeReq buyReq) {
        return sqlSession.update("TradeMapper.updateBalance", buyReq);
    }
}
