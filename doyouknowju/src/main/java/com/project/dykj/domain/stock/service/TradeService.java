package com.project.dykj.domain.stock.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.stock.dto.req.TradeReq;
import com.project.dykj.domain.stock.dto.res.TradeRes;
import com.project.dykj.domain.stock.dto.res.MyTradesRes;
import com.project.dykj.domain.stock.dto.res.TradesByStockIdRes;
import com.project.dykj.domain.stock.mapper.TradeMapper;

import com.project.dykj.domain.stock.exception.ErrorCode;
import com.project.dykj.domain.stock.exception.BusinessException;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeService {
    private final TradeMapper tradeMapper;

    public List<MyTradesRes> selectMyTrades(String userId) {
        return tradeMapper.selectMyTrades(userId);
    }

    public List<TradesByStockIdRes> selectTradesByStockId(String stockId) {
        return tradeMapper.selectTradesByStockId(stockId);
    }

    @Transactional
    public TradeRes buyStock(TradeReq tradeReq) {

        // 0. 잔액 확인
        if (tradeReq.getAfterBalance() < 0) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_BALANCE);
        }

        // 1. 거래 내역 추가
        int result1 = tradeMapper.insertTrade(tradeReq);

        if (result1 == 0) {
            throw new RuntimeException("거래 내역 추가에 실패했습니다.");
        }
        
        // 2. 보유 주식 수 업데이트

        // 만약 HOLDINGS 테이블에 (아이디, 종목코드) 쌍이 존재하지 않으면 insert
        // 존재하면 update

        // (아이디, 종목코드) 쌍 찾기
        int holdingCount = tradeMapper.selectHoldingCount(tradeReq);
        int result2;

        if (holdingCount == 0) {
            result2 = tradeMapper.insertHolding(tradeReq);
        }
        else {
            result2 = tradeMapper.updateHolding(tradeReq);
        }

        if (result2 == 0) {
            throw new RuntimeException("보유 주식 수 업데이트 실패");
        }

        // 3. 잔액 업데이트
        int result3 = tradeMapper.updateBalance(tradeReq);

        if (result3 == 0) {
            throw new RuntimeException("잔액 업데이트 실패");
        }

        // 4. 결과 리턴
        return TradeRes.builder()
                    .afterBalance(tradeReq.getAfterBalance())
                    .build();
    }

    // 주식 거래
    // 매수일 떄와 매도일 때 체크해야하는 조건이 다르다.
    @Transactional
    public TradeRes tradeStock(TradeReq tradeReq) {

        // 사용자 정보 가져오기
        return null;
    }

    @Transactional
    public int sellStock(TradeReq tradeReq) {
        return 0;
    }
}
