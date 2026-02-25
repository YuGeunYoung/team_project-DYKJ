package com.project.dykj.domain.trade_history.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.dykj.domain.trade_history.dto.req.PageReq;
import com.project.dykj.domain.trade_history.dto.res.TradeHistoryRes;
import com.project.dykj.domain.trade_history.mapper.TradeHistoryMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class TradeHistoryService {

    private final TradeHistoryMapper tradeHistoryMapper;
    
    public List<TradeHistoryRes> selectTradeHistory(String userId, int page, int groupSize) {

        PageReq pageReq = new PageReq();
        pageReq.setPage(page);
        pageReq.setGroupSize(groupSize);
        pageReq.setStart((page - 1) * pageReq.getGroupSize() + 1);
        pageReq.setEnd(page * pageReq.getGroupSize());

        List<TradeHistoryRes> result = tradeHistoryMapper.selectTradeHistory(userId, pageReq);

        return result;
    }

    public int selectTradeHistoryCount(String userId) {
        return tradeHistoryMapper.selectTradeHistoryCount(userId);
    }
}
