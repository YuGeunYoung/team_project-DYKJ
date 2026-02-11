package com.project.dykj.domain.stock.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.project.dykj.domain.stock.dto.req.UserIdAndStockIdReq;
import com.project.dykj.domain.stock.dto.res.GetFavoriteStocksRes;
import com.project.dykj.domain.stock.mapper.FavoriteStockMapper;
import com.project.dykj.kis.service.StockService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class FavoriteStockService {

    private final StockService stockService;
    private final FavoriteStockMapper favoriteStockMapper;

    public int addFavorite(UserIdAndStockIdReq req) {
        return favoriteStockMapper.addFavorite(req);
    }

    public int deleteFavorite(UserIdAndStockIdReq req) {
        return favoriteStockMapper.deleteFavorite(req);
    }

    public List<GetFavoriteStocksRes> getFavoriteStocks(String userId) {
        List<GetFavoriteStocksRes> res = favoriteStockMapper.getFavoriteStocks(userId);

        final int BATCH_SIZE = 30;
        Map<String, Object> prices = new HashMap<>();

        for (int i = 0; i < res.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, res.size());
            List<String> batchStockIds = res.subList(i, end).stream().map(GetFavoriteStocksRes::getStockId).toList();
            Map<String, Object> batchPrices = stockService.getMultiplePrices(batchStockIds);
            
            prices.putAll(batchPrices);

            try {
                Thread.sleep(750);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

        for (GetFavoriteStocksRes r : res) {
            @SuppressWarnings("unchecked")
            Map<String, Object> priceInfo = (Map<String, Object>) prices.get(r.getStockId());
            int currentPrice = Integer.parseInt(priceInfo.get("stck_prpr").toString());
            r.setStockPrice(currentPrice);
            r.setStockPriceChange(Integer.parseInt(priceInfo.get("prdy_vrss").toString()));
            r.setStockPriceChangeRate(Double.parseDouble(priceInfo.get("prdy_ctrt").toString()));
        }

        return res;
    }

    public int isFavorite(UserIdAndStockIdReq req) {
        return favoriteStockMapper.isFavorite(req);
    }
}
