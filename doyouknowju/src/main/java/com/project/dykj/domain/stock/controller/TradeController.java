package com.project.dykj.domain.stock.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.stock.dto.req.TradeReq;
import com.project.dykj.domain.stock.dto.res.TradeRes;
import com.project.dykj.domain.stock.service.TradeService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    // 자신의 거래 내역 조회
    @GetMapping("/my-trades")
    public ResponseEntity<?> getMyTrades(@RequestParam String userId) {
        return ResponseEntity.ok(tradeService.selectMyTrades(userId));
    }

    // 특정 종목의 거래 내역 조회
    @GetMapping("/trades/{stockId}")
    public ResponseEntity<?> getTradesByStockId(@PathVariable String stockId) {
        return ResponseEntity.ok(tradeService.selectTradesByStockId(stockId));
    }

    // 주식 매수
    @PostMapping("/buy")
    public ResponseEntity<?> buyStock(@RequestBody TradeReq tradeReq) {
        TradeRes buyRes = tradeService.buyStock(tradeReq);

        return ResponseEntity.ok(buyRes);
    }

    // 주식 매도
    @PostMapping("/sell")
    public ResponseEntity<?> sellStock(@RequestBody TradeReq tradeReq) {
        TradeRes sellRes = tradeService.tradeStock(tradeReq);

        return ResponseEntity.ok(sellRes);
    }
}
