package com.project.dykj.domain.stock.service;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.project.dykj.domain.notification.repository.HoldingMapper;
import com.project.dykj.domain.stock.dto.res.GetHoldingsByUserIdRes;
import com.project.dykj.domain.stock.vo.Holding;
import com.project.dykj.kis.service.StockService;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class HoldingService {
    
    private final HoldingMapper holdingMapper;
    private final StockService stockService;
    private final StockInfoService stockInfoService;

    public List<GetHoldingsByUserIdRes> getHoldingsByUserId(String userId) {
        List<Holding> holdings = holdingMapper.selectHoldingByUserId(userId);

        // holdings를 순회하면서 stockId를 이용해서 현재가를 가져와서 profitAndLoss, profitAndLossRate를 계산
        // 단, 그 수가 너무 커질 수 있으므로 최대 30개 씩 한 번에 가져올 수 있는 API를 사용
        
        // 20개씩 묶어서 현재가 가져오기
        List<String> stockIds = holdings.stream().map(Holding::getStockId).distinct().toList();
        Map<String, Object> prices = stockService.getMultiplePrices(stockIds);
        
        final int BATCH_SIZE = 30;

        for (int i = 0; i < stockIds.size(); i += BATCH_SIZE) {
            int end = Math.min(i + BATCH_SIZE, stockIds.size());
            List<String> batchStockIds = stockIds.subList(i, end);
            Map<String, Object> batchPrices = stockService.getMultiplePrices(batchStockIds);
            
            prices.putAll(batchPrices);
        }

        List<GetHoldingsByUserIdRes> result = new ArrayList<>();

        for (Holding holding : holdings) {
            GetHoldingsByUserIdRes res = new GetHoldingsByUserIdRes();

            @SuppressWarnings("unchecked")
            Map<String, Object> priceInfo = (Map<String, Object>) prices.get(holding.getStockId());
            long currentPrice = Long.parseLong(priceInfo.get("stck_prpr").toString());
            long profitAndLoss = currentPrice * holding.getTotalCount() - holding.getTotalPrice();
            double profitAndLossRate = (double) profitAndLoss / holding.getTotalPrice() * 100;
            res.setUserId(holding.getUserId());
            res.setStockId(holding.getStockId());
            res.setStockName(stockInfoService.getStockName(holding.getStockId()));
            res.setTotalPrice(holding.getTotalPrice());
            res.setTotalCount(holding.getTotalCount());
            res.setCurrentPrice(currentPrice);
            res.setProfitAndLoss(profitAndLoss);
            res.setProfitAndLossRate(profitAndLossRate);
            result.add(res);

            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                e.printStackTrace();
                Thread.currentThread().interrupt();
            }
        }

        
        return result;
    }
}
