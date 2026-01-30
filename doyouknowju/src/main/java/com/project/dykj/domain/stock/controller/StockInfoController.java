package com.project.dykj.domain.stock.controller;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.stock.service.StockInfoService;

import lombok.RequiredArgsConstructor;

@CrossOrigin(origins = "http://localhost:5173")
@RestController
@RequestMapping("/api/stock-info")
@RequiredArgsConstructor
public class StockInfoController {

    private final StockInfoService stockInfoService;

    @GetMapping("/{stockId}/name")
    public ResponseEntity<String> getStockName(@PathVariable String stockId) {
        return ResponseEntity.ok(stockInfoService.getStockName(stockId));
    }
}
