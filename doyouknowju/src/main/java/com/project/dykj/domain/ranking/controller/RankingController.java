package com.project.dykj.domain.ranking.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.ranking.dto.req.PageReq;
import com.project.dykj.domain.ranking.dto.res.AllRankingRes;
import com.project.dykj.domain.ranking.dto.res.RankingRes;
import com.project.dykj.domain.ranking.service.RankingService;

import java.util.List;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/ranking")
@RequiredArgsConstructor
public class RankingController {

    private final RankingService rankingService;

    @GetMapping("/season/{season:weekly|monthly|yearly}/{page}")
    public ResponseEntity<?> getSeasonRanking(@PathVariable String season, @PathVariable int page) {
        List<RankingRes> rankingResList = rankingService.getSeasonRanking(season.toUpperCase(), page);
        return ResponseEntity.ok(rankingResList);
    }
    
    @GetMapping("/weekly/{page}")
    public ResponseEntity<?> getWeeklyRanking(@PathVariable int page) {
        List<RankingRes> rankingResList = rankingService.getWeeklyRanking(page);
        return ResponseEntity.ok(rankingResList);
    }

    @GetMapping("/monthly/{page}")
    public ResponseEntity<?> getMonthlyRanking(@PathVariable int page) {
        List<RankingRes> rankingResList = rankingService.getMonthlyRanking(page);
        return ResponseEntity.ok(rankingResList);
    }

    @GetMapping("/yearly/{page}")
    public ResponseEntity<?> getYearlyRanking(@PathVariable int page) {
        List<RankingRes> rankingResList = rankingService.getYearlyRanking(page);
        return ResponseEntity.ok(rankingResList);
    }

    @GetMapping("/all/{page}")
    public ResponseEntity<?> getAllRanking(@PathVariable int page) {
        List<AllRankingRes> allRankingResList = rankingService.getAllRanking(page);
        return ResponseEntity.ok(allRankingResList);
    }
}
