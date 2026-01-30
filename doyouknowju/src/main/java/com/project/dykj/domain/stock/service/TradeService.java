package com.project.dykj.domain.stock.service;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.member.entity.Member;
import com.project.dykj.domain.member.mapper.MemberMapper;
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

    private final MemberMapper memberMapper;
    private final TradeMapper tradeMapper;

    public List<MyTradesRes> selectMyTrades(String userId) {
        return tradeMapper.selectMyTrades(userId);
    }

    public List<TradesByStockIdRes> selectTradesByStockId(String stockId) {
        return tradeMapper.selectTradesByStockId(stockId);
    }

    @Transactional
    public TradeRes buyStock(TradeReq tradeReq) {

        // 회원 정보 조회하기
        Member member = memberMapper.findByUserId(tradeReq.getUserId());

        // 회원 정보 조회 실패 시 NOT_FOUND_USER 에러 발생
        if (member == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_USER);
        }

        long afterBalance = member.getPoints() - tradeReq.getStockPrice() * tradeReq.getTradeCount();

        // 0. 잔액 확인
        if (afterBalance < 0) {
            throw new BusinessException(ErrorCode.NOT_ENOUGH_BALANCE);
        }

        tradeReq.setAfterBalance(afterBalance);

        // 1. 거래 내역 추가
        int result1 = tradeMapper.insertTrade(tradeReq);

        if (result1 == 0) {
            throw new BusinessException(ErrorCode.FAIL_TO_TRADE);
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
            result2 = tradeMapper.addHolding(tradeReq);
        }

        if (result2 == 0) {
            throw new BusinessException(ErrorCode.FAIL_TO_TRADE);
        }

        // 3. 잔액 업데이트
        int result3 = tradeMapper.updateBalance(tradeReq);

        if (result3 == 0) {
            throw new BusinessException(ErrorCode.FAIL_TO_TRADE);
        }

        // 4. 결과 리턴
        return TradeRes.builder()
                    .afterBalance(tradeReq.getAfterBalance())
                    .build();
    }

    // 주식 매도
    @Transactional
    public TradeRes sellStock(TradeReq tradeReq) {
        return null;
    }
}
