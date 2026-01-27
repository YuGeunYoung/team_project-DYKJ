package com.project.dykj.domain.stock.service;

import java.util.List;

import javax.management.RuntimeErrorException;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.stock.dto.BuyReq;
import com.project.dykj.domain.stock.dto.MyTradesRes;
import com.project.dykj.domain.stock.dto.TradesByStockIdRes;
import com.project.dykj.domain.stock.repository.TradeDao;

@Service
public class TradeService {

    @Autowired
    private TradeDao tradeDao;

    @Autowired
    private SqlSessionTemplate sqlSession;

    public List<MyTradesRes> selectMyTrades(String userId) {
        return tradeDao.selectMyTrades(sqlSession, userId);
    }

    public List<TradesByStockIdRes> selectTradesByStockId(String stockId) {
        return tradeDao.selectTradesByStockId(sqlSession, stockId);
    }

    @Transactional
    public int buyStock(BuyReq buyReq) {

        // 1. 거래 내역 추가
        int result1 = tradeDao.insertTrade(sqlSession, buyReq);

        if (result1 == 0) {
            throw new RuntimeException("거래 내역 추가 실패");
        }
        
        // 2. 보유 주식 수 업데이트

        // 만약 HOLDINGS 테이블에 (아이디, 종목코드) 쌍이 존재하지 않으면 insert
        // 존재하면 update

        // (아이디, 종목코드) 쌍 찾기
        int holdingCount = tradeDao.selectHoldingCount(sqlSession, buyReq);
        int result2;

        if (holdingCount == 0) {
            result2 = tradeDao.insertHolding(sqlSession, buyReq);
        }
        else {
            result2 = tradeDao.updateHolding(sqlSession, buyReq);
        }

        if (result2 == 0) {
            throw new RuntimeException("보유 주식 수 업데이트 실패");
        }

        // 3. 잔액 업데이트
        int result3 = tradeDao.updateBalance(sqlSession, buyReq);

        if (result3 == 0) {
            throw new RuntimeException("잔액 업데이트 실패");
        }

        return result1 * result2 * result3;
        
    }

    @Transactional
    public int sellStock(BuyReq buyReq) {
        return 0;
    }
}
