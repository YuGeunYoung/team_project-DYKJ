package com.project.dykj.kis.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.dykj.kis.model.vo.KisDailyChartResponse;
import com.project.dykj.kis.model.vo.StockSyncRequest;
import com.project.dykj.kis.model.vo.StockSuggestItem;
import com.project.dykj.kis.model.vo.StockUpsertRequest;
import com.project.dykj.kis.model.vo.VolumeRankItem;
import com.project.dykj.kis.ranking.MarketRankingService;
import com.project.dykj.kis.service.StockService;
import com.project.dykj.kis.service.StockService.MstImportResult;

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
     * Main page: volume TOP10
     */
    @GetMapping("/top10")
    public List<VolumeRankItem> volumeTop10() {
        return marketRankingService.getVolumeTop10();
    }

    /**
     * Stock autocomplete (suggest)
     */
    @GetMapping("/suggest")
    public List<StockSuggestItem> suggest(
            @RequestParam String q,
            @RequestParam(defaultValue = "10") int limit
    ) {
        return stockService.suggest(q, limit);
    }

    /**
     * Stock master (DB) lookup
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
     * Real-time price (KIS inquire-price)
     */
    @GetMapping("/{stockId}/price")
    public Map<?, ?> getPrice(@PathVariable String stockId) {
        return stockService.getCurrentPrice(stockId);
    }

    /**
     * Daily chart data (KIS inquire-daily-itemchartprice)
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
     * Detail: master + price + chart
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

    /**
     * Upsert STOCKS master directly
     */
    @PostMapping("/import")
    public ResponseEntity<Map<String, Object>> importStocks(@RequestBody List<StockUpsertRequest> items) {
        stockService.upsertStocks(items);
        return ResponseEntity.ok(Map.of("count", items == null ? 0 : items.size()));
    }

    /**
     * Fetch from KIS API and upsert into STOCKS
     */
    @PostMapping("/sync")
    public ResponseEntity<Map<String, Object>> syncFromKis(@RequestBody StockSyncRequest req) {
        List<String> ids = req == null ? List.of() : req.getStockIds();
        String prdtTypeCd = req == null ? null : req.getPrdtTypeCd();
        stockService.syncFromKis(ids, prdtTypeCd);
        return ResponseEntity.ok(Map.of("count", ids == null ? 0 : ids.size()));
    }

    /**
     * Import KOSPI code MST file and upsert into STOCKS
     */
    @PostMapping(value = "/import-mst/kospi", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> importKospiCodeMst(
            @RequestPart("file") MultipartFile file,
            @RequestParam(defaultValue = "MS949") String encoding,
            @RequestParam(defaultValue = "true") boolean onlyStocks
    ) {
        MstImportResult result = stockService.importKospiCodeMst(
                asInputStream(file),
                java.nio.charset.Charset.forName(encoding),
                onlyStocks
        );
        return ResponseEntity.ok(Map.of(
                "totalLines", result.totalLines(),
                "imported", result.imported(),
                "skippedNotStock", result.skippedNotStock(),
                "skippedInvalid", result.skippedInvalid()
        ));
    }

    private static java.io.InputStream asInputStream(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("file is required");
        }
        try {
            return file.getInputStream();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("failed to read upload", e);
        }
    }
}

