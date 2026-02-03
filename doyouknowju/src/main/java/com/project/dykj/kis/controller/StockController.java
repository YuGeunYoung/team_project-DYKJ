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

    public StockController(StockService stockService, MarketRankingService marketRankingService) {
        this.stockService = stockService;
        this.marketRankingService = marketRankingService;
    }

    /**
     * 메인 페이지용: 거래량 TOP10 조회
     * - 내부적으로 KIS 거래량 랭킹 API를 호출하여 상위 10개를 반환합니다.
     * - KIS는 초당 제한이 있으므로(레이트리밋) 과도한 폴링/중복 호출은 500(EGW00201)을 유발할 수 있습니다.
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
     * 자동완성(추천) 목록 조회
     * - q(prefix)로 시작하는 종목명/종목코드를 DB(STOCKS)에서 빠르게 조회합니다.
     * - 입력 중 계속 호출될 수 있으므로 limit를 작게 유지하는 것을 권장합니다.
     */
    @GetMapping("/suggest")
    public List<StockSuggestItem> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return stockService.suggest(q, limit);
    }

    /**
     * 검색 결과 페이지용 목록 조회
     * - q(키워드)가 포함(contains)된 종목을 DB(STOCKS)에서 조회합니다.
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
     * 리스트 페이지용 복수 현재가 조회 (최대 20개)
     * - 검색 결과 페이지에서 종목별로 /price를 N번 호출하면 KIS 레이트리밋에 걸리기 쉬워서,
     *   복수 조회 API(intstock-multprice)로 한 번에 조회하도록 만든 엔드포인트입니다.
     * - 요청 바디 예시:
     *   1) { "stockIds": ["005930","000660"] }
     *   2) ["005930","000660"]
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
     * - STOCKS 테이블에 적재된 "고정 정보"(이름/섹터/설명/활성여부 등)를 반환합니다.
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
     * - 단건 조회는 대부분의 종목코드를 처리하지만, 과도한 호출은 레이트리밋(EGW00201)에 걸릴 수 있습니다.
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
     * 상세 페이지용 묶음 조회
     * - master(DB) + price(KIS) + chart(KIS)를 한 번에 반환합니다.
     * - 차트 설정(kis.daily-chart.tr-id)이 비어있으면 차트 호출에서 에러가 날 수 있습니다.
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
