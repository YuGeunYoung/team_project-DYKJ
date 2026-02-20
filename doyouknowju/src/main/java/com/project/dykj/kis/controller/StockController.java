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

    /** 椰꾧퀡???TOP10 鈺곌퀬??*/
    @GetMapping("/top10")
    public List<VolumeRankItem> volumeTop10() {
        return marketRankingService.getVolumeTop10();
    }

    /** ??쇱뵠甕?椰꾧퀡???疫?TOP10 鈺곌퀬??*/
    @GetMapping("/top10/trade-value/naver")
    public List<NaverTradeValueRankItem> naverTradeValueTop10() {
        return naverRankingService.getTradeValueTop10();
    }

    /** ????μ빘釉?TOP10 鈺곌퀬??*/
    @GetMapping("/top10/market-cap")
    public List<MarketCapRankItem> marketCapTop10() {
        return marketRankingService.getMarketCapTop10();
    }

    /** ?癒?짗?袁⑷쉐 筌뤴뫖以?鈺곌퀬??*/
    @GetMapping("/suggest")
    public List<StockSuggestItem> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return stockService.suggest(q, limit);
    }

    /** 野꺜??野껉퀗??筌뤴뫖以?鈺곌퀬??*/
    @GetMapping("/search")
    public List<StockSearchItem> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "30") int size,
            HttpSession session //[taek] : 嚥≪뮄????類ｋ궖 ?븍뜄???session ?곕떽?
    ) {
    	//[taek] : ?醫? ?袁⑹뵠???븍뜄???삳뮉 ?꾨뗀諭??곕떽?
    	String userId = null;
    	Member loginUser = (Member)session.getAttribute("loginUser");
    	
    	if(loginUser != null) {
    		userId = loginUser.getUserId();
    	}
        return stockService.search(q, page, size, userId);
    }

    /** ?귐딅뮞???遺얇늺??癰귣벊???袁⑹삺揶쎛 鈺곌퀬??*/
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

    /** ?ル굝??筌띾뜆???DB) ??ｊ탷 鈺곌퀬??*/
    @GetMapping("/{stockId}/master")
    public ResponseEntity<StockUpsertRequest> getMaster(@PathVariable String stockId) {
        StockUpsertRequest master = stockService.findById(stockId);
        if (master == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(master);
    }

    /** ??쇰뻻揶??袁⑹삺揶쎛 鈺곌퀬??*/
    @GetMapping("/{stockId}/price")
    public Map<?, ?> getPrice(@PathVariable String stockId) {
        return stockService.getCurrentPrice(stockId);
    }

    /** ??雅???筌△뫂??鈺곌퀬??*/
    @GetMapping("/{stockId}/chart/daily")
    public KisDailyChartResponse getDailyChart(
            @PathVariable String stockId,
            @RequestParam(required = false) String start,
            @RequestParam(required = false) String end,
            @RequestParam(required = false) String period
    ) {
        return stockService.getDailyChart(stockId, start, end, period);
    }

    /** ?ル굝???怨멸쉭 ?얜씈??鈺곌퀬??(master + price + chart) */
    /** ?꾨뗄???筌왖??筌△뫂??鈺곌퀬??(0001) */
    @GetMapping("/index/kospi/chart")
    public Map<?, ?> getKospiChart(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) String startDateTime,
            @RequestParam(required = false) String endDateTime
    ) {
        Map<String, Object> naver = naverIndexChartService.getKospiChart(range, startDateTime, endDateTime);
        if (naver != null && !naver.isEmpty()) {
            return naver;
        }
        return Map.of("rt_cd", "1", "msg_cd", "NO_DATA", "msg1", "?꾨뗄???筌△뫂???怨쀬뵠?怨? ??곷뮸??덈뼄.", "output", List.of());
    }

    /** ?꾨뗄???筌왖??筌△뫂??鈺곌퀬??(1001) */
    @GetMapping("/index/kosdaq/chart")
    public Map<?, ?> getKosdaqChart(
            @RequestParam(required = false) String range,
            @RequestParam(required = false) String startDateTime,
            @RequestParam(required = false) String endDateTime
    ) {
        Map<String, Object> naver = naverIndexChartService.getKosdaqChart(range, startDateTime, endDateTime);
        if (naver != null && !naver.isEmpty()) {
            return naver;
        }
        return Map.of("rt_cd", "1", "msg_cd", "NO_DATA", "msg1", "?꾨뗄???筌△뫂???怨쀬뵠?怨? ??곷뮸??덈뼄.", "output", List.of());
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

    /** ?源낆뵭???怨몃뱟 TOP10 鈺곌퀬??*/
    @GetMapping("/top10/rise-rate")
    public List<RiseFallRankItem> riseRateTop10() {
        return marketRankingService.getRiseRateTop10();
    }

    /** ?源낆뵭????롮뵭 TOP10 鈺곌퀬??*/
    @GetMapping("/top10/fall-rate")
    public List<RiseFallRankItem> fallRateTop10() {
        return marketRankingService.getFallRateTop10();
    }
}

