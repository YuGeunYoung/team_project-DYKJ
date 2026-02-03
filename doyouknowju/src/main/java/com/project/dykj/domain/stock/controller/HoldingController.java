package com.project.dykj.domain.stock.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.stock.service.HoldingService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/holdings")
@RequiredArgsConstructor
public class HoldingController {

    private final HoldingService holdingService; 
    // 특정 유저의 보유 주식 현황 가져오기
    @GetMapping("/{userId}")
    public ResponseEntity<?> getHoldingsByUserId(@PathVariable String userId) {
        return ResponseEntity.ok(holdingService.getHoldingsByUserId(userId));
    }
}
