package com.project.dykj.domain.stock.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.project.dykj.domain.stock.dto.req.UserIdAndStockIdReq;
import com.project.dykj.domain.stock.dto.res.GetFavoriteStocksRes;
import com.project.dykj.domain.stock.mapper.FavoriteStockMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteStockService {

    private final FavoriteStockMapper favoriteStockMapper;

    public int addFavorite(UserIdAndStockIdReq req) {
        return favoriteStockMapper.addFavorite(req);
    }

    public int deleteFavorite(UserIdAndStockIdReq req) {
        return favoriteStockMapper.deleteFavorite(req);
    }

    public List<GetFavoriteStocksRes> getFavoriteStocks(String userId) {
        return favoriteStockMapper.getFavoriteStocks(userId);
    }

    public int isFavorite(UserIdAndStockIdReq req) {
        return favoriteStockMapper.isFavorite(req);
    }
}
