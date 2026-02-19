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

import com.project.dykj.domain.member.entity.Member;
import com.project.dykj.kis.model.vo.KisDailyChartResponse;
import com.project.dykj.kis.model.vo.MarketCapRankItem;
import com.project.dykj.kis.model.vo.NaverTradeValueRankItem;
import com.project.dykj.kis.model.vo.RiseFallRankItem;
import com.project.dykj.kis.model.vo.StockSearchItem;
import com.project.dykj.kis.model.vo.StockSuggestItem;
import com.project.dykj.kis.model.vo.StockUpsertRequest;
import com.project.dykj.kis.model.vo.VolumeRankItem;
import com.project.dykj.kis.ranking.MarketRankingService;
import com.project.dykj.kis.service.NaverIndexChartService;
import com.project.dykj.kis.service.NaverRankingService;
import com.project.dykj.kis.service.StockService;

import jakarta.servlet.http.HttpSession;

@RestController
@RequestMapping("/api/stocks")
public class StockController {

    private final StockService stockService;
    private final MarketRankingService marketRankingService;
    private final NaverRankingService naverRankingService;
    private final NaverIndexChartService naverIndexChartService;

    public StockController(
            StockService stockService,
            MarketRankingService marketRankingService,
            NaverRankingService naverRankingService,
            NaverIndexChartService naverIndexChartService
    ) {
        this.stockService = stockService;
        this.marketRankingService = marketRankingService;
        this.naverRankingService = naverRankingService;
        this.naverIndexChartService = naverIndexChartService;
    }

    /** 嫄곕옒??TOP10 議고쉶 */
    @GetMapping("/top10")
    public List<VolumeRankItem> volumeTop10() {
        return marketRankingService.getVolumeTop10();
    }

    /** ?ㅼ씠踰?嫄곕옒?湲?TOP10 議고쉶 */
    @GetMapping("/top10/trade-value/naver")
    public List<NaverTradeValueRankItem> naverTradeValueTop10() {
        return naverRankingService.getTradeValueTop10();
    }

    /** ?쒓?珥앹븸 TOP10 議고쉶 */
    @GetMapping("/top10/market-cap")
    public List<MarketCapRankItem> marketCapTop10() {
        return marketRankingService.getMarketCapTop10();
    }

    /** ?먮룞?꾩꽦 紐⑸줉 議고쉶 */
    @GetMapping("/suggest")
    public List<StockSuggestItem> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return stockService.suggest(q, limit);
    }

    /** 寃??寃곌낵 紐⑸줉 議고쉶 */
    @GetMapping("/search")
    public List<StockSearchItem> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size,
            HttpSession session //[taek] : 濡쒓렇???뺣낫 遺덈윭??session 異붽?
    ) {
    	//[taek] : ?좎? ?꾩씠??遺덈윭?ㅻ뒗 肄붾뱶 異붽?
    	String userId = null;
    	Member loginUser = (Member)session.getAttribute("loginUser");
    	
    	if(loginUser != null) {
    		userId = loginUser.getUserId();
    	}
        return stockService.search(q, page, size, userId);
    }

    /** 由ъ뒪???붾㈃??蹂듭닔 ?꾩옱媛 議고쉶 */
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

    /** 醫낅ぉ 留덉뒪??DB) ?④굔 議고쉶 */
    @GetMapping("/{stockId}/master")
    public ResponseEntity<StockUpsertRequest> getMaster(@PathVariable String stockId) {
        StockUpsertRequest master = stockService.findById(stockId);
        if (master == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(master);
    }

    /** ?ㅼ떆媛??꾩옱媛 議고쉶 */
    @GetMapping("/{stockId}/price")
    public Map<?, ?> getPrice(@PathVariable String stockId) {
        return stockService.getCurrentPrice(stockId);
    }

    /** ??二???李⑦듃 議고쉶 */
    @GetMapping("/{stockId}/chart/daily")
    public KisDailyChartResponse getDailyChart(
            @PathVariable String stockId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String period
    ) {
        return stockService.getDailyChart(stockId, start, end, period);
    }

    /** 醫낅ぉ ?곸꽭 臾띠쓬 議고쉶 (master + price + chart) */
    /** 肄붿뒪??吏??李⑦듃 議고쉶 (0001) */
    @GetMapping("/index/kospi/chart")
    public Map<?, ?> getKospiChart() {
        Map<String, Object> naver = naverIndexChartService.getKospiChart();
        if (naver != null && !naver.isEmpty()) {
            return naver;
        }
        return Map.of("rt_cd", "1", "msg_cd", "NO_DATA", "msg1", "肄붿뒪??李⑦듃 ?곗씠?곌? ?놁뒿?덈떎.", "output", List.of());
    }

    /** 肄붿뒪??吏??李⑦듃 議고쉶 (1001) */
    @GetMapping("/index/kosdaq/chart")
    public Map<?, ?> getKosdaqChart() {
        Map<String, Object> naver = naverIndexChartService.getKosdaqChart();
        if (naver != null && !naver.isEmpty()) {
            return naver;
        }
        return Map.of("rt_cd", "1", "msg_cd", "NO_DATA", "msg1", "肄붿뒪??李⑦듃 ?곗씠?곌? ?놁뒿?덈떎.", "output", List.of());
    }

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

    /** ?깅씫瑜??곸듅 TOP10 議고쉶 */
    @GetMapping("/top10/rise-rate")
    public List<RiseFallRankItem> riseRateTop10() {
        return marketRankingService.getRiseRateTop10();
    }

    /** ?깅씫瑜??섎씫 TOP10 議고쉶 */
    @GetMapping("/top10/fall-rate")
    public List<RiseFallRankItem> fallRateTop10() {
        return marketRankingService.getFallRateTop10();
    }
}

