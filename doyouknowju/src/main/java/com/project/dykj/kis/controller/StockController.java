package com.project.dykj.kis.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.kis.model.vo.KisDailyChartResponse;
import com.project.dykj.kis.model.vo.MarketCapRankItem;
import com.project.dykj.kis.model.vo.NaverTradeValueRankItem;
import com.project.dykj.kis.model.vo.RiseFallRankItem;
import com.project.dykj.kis.model.vo.StockSearchItem;
import com.project.dykj.kis.model.vo.StockSuggestItem;
import com.project.dykj.kis.model.vo.StockUpsertRequest;
import com.project.dykj.kis.model.vo.VolumeRankItem;
import com.project.dykj.kis.ranking.MarketRankingService;
import com.project.dykj.kis.service.NaverRankingService;
import com.project.dykj.kis.service.StockService;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;
    private final MarketRankingService marketRankingService;
    private final NaverRankingService naverRankingService;

    public StockController(
            StockService stockService,
            MarketRankingService marketRankingService,
            NaverRankingService naverRankingService
    ) {
        this.stockService = stockService;
        this.marketRankingService = marketRankingService;
        this.naverRankingService = naverRankingService;
    }

    /** 거래량 TOP10 조회 */
    @GetMapping("/top10")
    public List<VolumeRankItem> volumeTop10() {
        return marketRankingService.getVolumeTop10();
    }

    /** 네이버 거래대금 TOP10 조회 */
    @GetMapping("/top10/trade-value/naver")
    public List<NaverTradeValueRankItem> naverTradeValueTop10() {
        return naverRankingService.getTradeValueTop10();
    }

    /** 시가총액 TOP10 조회 */
    @GetMapping("/top10/market-cap")
    public List<MarketCapRankItem> marketCapTop10() {
        return marketRankingService.getMarketCapTop10();
    }

    /** 자동완성 목록 조회 */
    @GetMapping("/suggest")
    public List<StockSuggestItem> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return stockService.suggest(q, limit);
    }

    /** 검색 결과 목록 조회 */
    @GetMapping("/search")
    public List<StockSearchItem> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return stockService.search(q, page, size);
    }

    /** 리스트 화면용 복수 현재가 조회 */
    @PostMapping("/prices")
    public Map<String, Object> getMultiplePrices(@RequestBody(required = false) Object body) {
        List<String> ids = extractStockIds(body);
        return stockService.getMultiplePrices(ids);
    }

    @SuppressWarnings("unchecked")
    private static List<String> extractStockIds(Object body) {
        if (body == null) {
            return List.of();
        }
        Object rawIds = null;
        if (body instanceof Map<?, ?> map) {
            rawIds = map.get("stockIds");
            if (rawIds == null) {
                rawIds = map.get("stock_ids");
            }
        } else if (body instanceof List<?> list) {
            rawIds = list;
        }

        if (!(rawIds instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(v -> v != null && !String.valueOf(v).trim().isEmpty())
                .map(v -> String.valueOf(v).trim())
                .toList();
    }

    /** 종목 마스터(DB) 단건 조회 */
    @GetMapping("/{stockId}/master")
    public ResponseEntity<StockUpsertRequest> getMaster(@PathVariable String stockId) {
        StockUpsertRequest master = stockService.findById(stockId);
        if (master == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(master);
    }

    /** 실시간 현재가 조회 */
    @GetMapping("/{stockId}/price")
    public Map<?, ?> getPrice(@PathVariable String stockId) {
        return stockService.getCurrentPrice(stockId);
    }

    /** 일/주/월 차트 조회 */
    @GetMapping("/{stockId}/chart/daily")
    public KisDailyChartResponse getDailyChart(
            @PathVariable String stockId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String period
    ) {
        return stockService.getDailyChart(stockId, start, end, period);
    }

    /** 종목 상세 묶음 조회 (master + price + chart) */
    @GetMapping("/{stockId}/detail")
    public Map<String, Object> getDetail(
            @PathVariable String stockId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String period
    ) {
        StockUpsertRequest master = stockService.findById(stockId);
        Map<?, ?> price = stockService.getCurrentPrice(stockId);
        KisDailyChartResponse chart = stockService.getDailyChart(stockId, start, end, period);
        return Map.of(
                "master", master,
                "price", price,
                "chart", chart
        );
    }

    /** 등락률 상승 TOP10 조회 */
    @GetMapping("/top10/rise-rate")
    public List<RiseFallRankItem> riseRateTop10() {
        return marketRankingService.getRiseRateTop10();
    }

    /** 등락률 하락 TOP10 조회 */
    @GetMapping("/top10/fall-rate")
    public List<RiseFallRankItem> fallRateTop10() {
        return marketRankingService.getFallRateTop10();
    }
}
