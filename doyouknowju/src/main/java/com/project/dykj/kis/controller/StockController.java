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

import com.project.dykj.kis.model.vo.NaverTradeValueRankItem;
import com.project.dykj.kis.service.NaverRankingService;
import com.project.dykj.domain.member.entity.Member;
import com.project.dykj.kis.model.vo.KisDailyChartResponse;
import com.project.dykj.kis.model.vo.MarketCapRankItem;
import com.project.dykj.kis.model.vo.RiseFallRankItem;
import com.project.dykj.kis.model.vo.StockSearchItem;
import com.project.dykj.kis.model.vo.StockSuggestItem;
import com.project.dykj.kis.model.vo.StockUpsertRequest;
import com.project.dykj.kis.model.vo.TradeAmountRankItem;
import com.project.dykj.kis.model.vo.VolumeRankItem;
import com.project.dykj.kis.ranking.MarketRankingService;
import com.project.dykj.kis.service.StockService;

import jakarta.servlet.http.HttpSession;

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
     * 메인페이지용 거래량 TOP10 조회
     * - KIS 거래량 순위 API를 호출해 상위 10개를 반환합니다.
     * - KIS는 초당 호출 제한(EGW00201)이 있어, 프론트에서 폴링/중복 호출 시 500이 날 수 있습니다.
     *   (백엔드에서 짧은 캐시로 보호하고 있습니다.)
     */
    @GetMapping("/top10")
    public List<VolumeRankItem> volumeTop10() {
        return marketRankingService.getVolumeTop10();
    }

    /**
     * 거래대금 Top10
     * - 현재 구현은 KIS 거래량 순위 API의 표본(최대 30건) 안에서 누적 거래대금 필드를 기준으로 정렬해 Top10을 만듭니다.
     * - 따라서 "시장 전체 거래대금 Top10"과는 오차가 있을 수 있습니다.
     */
    @GetMapping("/top10/trade-amount")
    public List<TradeAmountRankItem> tradeAmountTop10() {
        return marketRankingService.getTradeAmountTop10();
    }

    /**
     * 네이버(증권) 거래대금 Top10 조회
     * - stock.naver.com 내부 JSON API를 호출해 표본(pageSize=100)에서 누적 거래대금(accAmount) 기준 Top10을 만듭니다.
     * - 외부 API 호출 폭발을 막기 위해 서버에서 캐시(TTL)를 사용합니다.
     */
    @GetMapping("/top10/trade-value/naver")
    public List<NaverTradeValueRankItem> naverTradeValueTop10() {
        return naverRankingService.getTradeValueTop10();
    }

    /**
     * 자동완성(추천) 목록 조회
     * - q(prefix)로 시작하는 종목명/종목코드를 DB(STOCKS)에서 조회합니다.
     * - 입력 중 계속 호출될 수 있으므로 limit은 작게 두는 것을 권장합니다.
     */
    /** 시가총액 TOP10 조회 (KIS) */
    @GetMapping("/top10/market-cap")
    public List<MarketCapRankItem> marketCapTop10() {
        return marketRankingService.getMarketCapTop10();
    }

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
            @RequestParam(defaultValue = "30") int size,
            HttpSession session //[taek] : 로그인 정보 불러올 session 추가
    ) {
    	//[taek] : 유저 아이디 불러오는 코드 추가
    	String userId = null;
    	Member loginUser = (Member)session.getAttribute("loginUser");
    	
    	if(loginUser != null) {
    		userId = loginUser.getUserId();
    	}
        return stockService.search(q, page, size, userId);
    }

    /**
     * 리스트 페이지용 복수 현재가 조회
     * - 검색 결과 리스트에서 종목별로 /price를 N번 호출하면 KIS 호출 제한에 걸리기 쉬우므로,
     *   가능한 한 번의 호출로 여러 종목의 현재가를 조회합니다.
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
     * - STOCKS 테이블에 적재된 "고정 정보"(종목명/섹터/설명/활성 여부 등)를 반환합니다.
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
     * 실시간 현재가 조회(KIS inquire-price)
     * - KIS는 초당 호출 제한(EGW00201)이 있어, 과도한 호출 시 실패할 수 있습니다.
     */
    @GetMapping("/{stockId}/price")
    public Map<?, ?> getPrice(@PathVariable String stockId) {
        return stockService.getCurrentPrice(stockId);
    }

    /**
     * 일/주/월 차트 데이터 조회(KIS inquire-daily-itemchartprice)
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
     * - 차트 설정(kis.daily-chart.tr-id)이 비어있으면 차트 호출에서 오류가 날 수 있습니다.
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

    /** 등락률(상승) TOP10 조회 */
    @GetMapping("/top10/rise-rate")
    public List<RiseFallRankItem> riseRateTop10() {
        return marketRankingService.getRiseRateTop10();
    }

    /** 등락률(하락) TOP10 조회 */
    @GetMapping("/top10/fall-rate")
    public List<RiseFallRankItem> fallRateTop10() {
        return marketRankingService.getFallRateTop10();
    }

}
