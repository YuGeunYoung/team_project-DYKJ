package com.project.dykj.domain.stock.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.stock.dto.BuyReq;
import com.project.dykj.domain.stock.dto.MyTradesRes;
import com.project.dykj.domain.stock.dto.TradesByStockIdRes;
import com.project.dykj.domain.stock.service.TradeService;

@RestController
@RequestMapping("/trade")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    // 자신의 거래 내역 조회
    @GetMapping("/my-trades")
    public ResponseEntity<?> getMyTrades(String userId) {
        List<MyTradesRes> myTrades = tradeService.selectMyTrades(userId);

        if (myTrades != null) {
            return ResponseEntity.ok(myTrades);
        }
        else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("fail");
        }
    }

    // 특정 종목의 거래 내역 조회
    @GetMapping("/trades/{stockId}")
    public ResponseEntity<?> getTradesByStockId(@PathVariable String stockId) {
        List<TradesByStockIdRes> tradesByStockId = tradeService.selectTradesByStockId(stockId);

        if (tradesByStockId != null) {
            return ResponseEntity.ok(tradesByStockId);
        }
        else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("fail");
        }
    }

    // 주식 매수
    @PostMapping("/buy")
    public ResponseEntity<?> buyStock(@RequestBody BuyReq buyReq) {
        int result = tradeService.buyStock(buyReq);

        if (result > 0) {
            return ResponseEntity.ok("success");
        }
        else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("fail");
        }
    }

    // 주식 매도
    @PostMapping("/sell")
    public ResponseEntity<?> sellStock(@RequestBody BuyReq buyReq) {
        int result = tradeService.sellStock(buyReq);

        if (result > 0) {
            return ResponseEntity.ok("success");
        }
        else {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("fail");
        }
    }
}
