package com.project.dykj.domain.ranking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.ranking.dto.req.PageReq;

@RestController
@RequestMapping("/api/ranking")
public class RankingController {
    
    @GetMapping("/weekly")
    public ResponseEntity<?> getWeeklyRanking(@RequestBody PageReq pageReq) {
        return ResponseEntity.ok("getWeeklyRanking");
    }

    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyRanking(@RequestBody PageReq pageReq) {
        return ResponseEntity.ok("getMonthlyRanking");
    }

    @GetMapping("/yearly")
    public ResponseEntity<?> getYearlyRanking(@RequestBody PageReq pageReq) {
        return ResponseEntity.ok("getYearlyRanking");
    }

    @GetMapping("/all")
    public ResponseEntity<?> getAllRanking(@RequestBody PageReq pageReq) {
        return ResponseEntity.ok("getAllRanking");
    }
}
