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

import com.project.dykj.domain.stock.dto.res.NaverTradeValueRankItem;
import com.project.dykj.domain.stock.service.NaverRankingService;
import com.project.dykj.kis.model.vo.KisDailyChartResponse;
import com.project.dykj.kis.model.vo.StockSearchItem;
import com.project.dykj.kis.model.vo.StockSuggestItem;
import com.project.dykj.kis.model.vo.StockUpsertRequest;
import com.project.dykj.kis.model.vo.TradeAmountRankItem;
import com.project.dykj.kis.model.vo.VolumeRankItem;
import com.project.dykj.kis.ranking.MarketRankingService;
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

    /**
     * 메인 페이지용: 거래량 TOP10 조회
     * - 기본적으로 KIS 거래량 순위 API를 호출해 상위 10개를 반환합니다.
     * - KIS는 초당 호출 제한이 있으므로(예: EGW00201) 과도한 폴링/중복 호출은 피해야 합니다.
     */
    @GetMapping("/top10")
    public List<VolumeRankItem> volumeTop10() {
        return marketRankingService.getVolumeTop10();
    }

    /**
     * 거래대금 Top10 (거래량 순위 API의 표본 30건 내에서 재정렬)
     */
    @GetMapping("/top10/trade-amount")
    public List<TradeAmountRankItem> tradeAmountTop10() {
        return marketRankingService.getTradeAmountTop10();
    }

    /**
     * 네이버(내부 JSON API) 기반 거래대금 Top10
     * - priceTop 페이지의 랭킹 목록(pageSize=100)을 받아 누적 거래대금(accAmount) 기준으로 재정렬해 상위 10개를 반환합니다.
     * - 외부 API를 과도하게 호출하지 않도록 서버에서 짧은 캐시(TTL)를 사용합니다.
     */
    @GetMapping("/top10/trade-value/naver")
    public List<NaverTradeValueRankItem> naverTradeValueTop10() {
        return naverRankingService.getTradeValueTop10();
    }

    /**
     * 자동완성(추천) 목록 조회
     * - q(prefix)로 시작하는 종목명/종목코드를 DB(STOCKS)에서 조회합니다.
     * - 입력 중 계속 호출되므로 limit을 너무 크게 두지 않는 것을 권장합니다.
     */
    @GetMapping("/suggest")
    public List<StockSuggestItem> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return stockService.suggest(q, limit);
    }

    /**
     * 검색 결과(리스트) 조회
     * - q(keyword)가 포함된 종목을 DB(STOCKS)에서 조회합니다.
     * - page/size로 페이지네이션을 지원합니다(기본 size=30).
     */
    @GetMapping("/search")
    public List<StockSearchItem> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size
    ) {
        return stockService.search(q, page, size);
    }

    /**
     * 리스트 페이지용: 복수 현재가 조회
     * - 검색결과 페이지에서 종목별로 /price를 N번 호출하면 KIS 호출 제한에 걸리기 쉬워, 복수 조회로 한 번에 조회합니다.
     *
     * 요청 바디 예시:
     * 1) { "stockIds": ["005930","000660"] }
     * 2) ["005930","000660"]
     */
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

    /**
     * 종목 마스터(DB) 단건 조회
     * - STOCKS 테이블에 적재된 "고정 정보"(이름/섹터/설명/활성 여부 등)를 반환합니다.
     */
    @GetMapping("/{stockId}/master")
    public ResponseEntity<StockUpsertRequest> getMaster(@PathVariable String stockId) {
        StockUpsertRequest master = stockService.findById(stockId);
        if (master == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(master);
    }

    /**
     * 실시간 현재가 조회 (KIS inquire-price)
     * - 과도한 호출은 KIS 레이트리밋(EGW00201)에 걸릴 수 있습니다.
     */
    @GetMapping("/{stockId}/price")
    public Map<?, ?> getPrice(@PathVariable String stockId) {
        return stockService.getCurrentPrice(stockId);
    }

    /**
     * 일/주/월 차트 데이터 조회 (KIS inquire-daily-itemchartprice)
     * - start/end: YYYYMMDD
     * - period: D/W/M
     */
    @GetMapping("/{stockId}/chart/daily")
    public KisDailyChartResponse getDailyChart(
            @PathVariable String stockId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String period
    ) {
        return stockService.getDailyChart(stockId, start, end, period);
    }

    /**
     * 상세 페이지용: 묶음 조회
     * - master(DB) + price(KIS) + chart(KIS)를 한 번에 반환합니다.
     * - 차트 설정(kis.daily-chart.tr-id)이 비어있으면 차트 호출에서 오류가 발생할 수 있습니다.
     */
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

}