package com.project.dykj.domain.stock.controller;

import java.util.List;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.stock.dto.req.UserIdAndStockIdReq;
import com.project.dykj.domain.stock.dto.res.GetFavoriteStocksRes;
import com.project.dykj.domain.stock.service.FavoriteStockService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/favorite-stock")
@RequiredArgsConstructor
public class FavoriteStockController {

    private final FavoriteStockService favoriteStockService;
    
    @PostMapping("/add")
    public ResponseEntity<?> addFavorite(@RequestBody UserIdAndStockIdReq req) {
        int res = favoriteStockService.addFavorite(req);
        return ResponseEntity.ok(res);
    }

    @PostMapping("/delete")
    public ResponseEntity<?> deleteFavorite(@RequestBody UserIdAndStockIdReq req) {
        int res = favoriteStockService.deleteFavorite(req);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/list/{userId}")
    public ResponseEntity<?> getFavoriteStocks(@PathVariable String userId) {
        List<GetFavoriteStocksRes> res = favoriteStockService.getFavoriteStocks(userId);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/check/{userId}/{stockId}")
    public ResponseEntity<?> isFavorite(@PathVariable String userId, @PathVariable String stockId) {
        UserIdAndStockIdReq req = new UserIdAndStockIdReq(userId, stockId);
        int res = favoriteStockService.isFavorite(req);
        return ResponseEntity.ok(res);
    }
}
