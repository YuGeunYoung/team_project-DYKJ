package com.project.dykj.domain.stock.controller;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.stock.dto.MyTradesRes;
import com.project.dykj.domain.stock.service.TradeService;

@RestController
@RequestMapping("/trade")
public class TradeController {

    @Autowired
    private TradeService tradeService;

    // 자신의 거래 내역 조회
    @GetMapping("/my-trades")
    public List<MyTradesRes> getMyTrades() {
        List<MyTradesRes> myTrades = tradeService.selectMyTrades("user01");
        return myTrades;
    }

    // 특정 종목의 거래 내역 조회
    @GetMapping("/trades/{stockId}")
    public String getTradesByStockId(@PathVariable String stockId) {
        return "trade/trades";
    }

    // 주식 매수
    @PostMapping("/buy")
    public String buyStock() {
        return "trade/buy-stock";
    }

    // 주식 매도
    @PostMapping("/sell")
    public String sellStock() {
        return "trade/sell-stock";
    }
}
