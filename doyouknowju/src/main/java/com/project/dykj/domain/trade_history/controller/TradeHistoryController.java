package com.project.dykj.domain.trade_history.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.trade_history.service.TradeHistoryService;

import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trade-history")
public class TradeHistoryController {
    
    private final TradeHistoryService tradeHistoryService;
    
    @GetMapping("/{userId}/{page}/{groupSize}")
    public ResponseEntity<?> selectTradeHistory(@PathVariable String userId,
                                                    @PathVariable int page,
                                                    @PathVariable int groupSize) {
        return ResponseEntity.ok(tradeHistoryService.selectTradeHistory(userId, page, groupSize));
    }

    @GetMapping("/{userId}/count")
    public ResponseEntity<?> selectTradeHistoryCount(@PathVariable String userId) {
        return ResponseEntity.ok(tradeHistoryService.selectTradeHistoryCount(userId));
    }
}
