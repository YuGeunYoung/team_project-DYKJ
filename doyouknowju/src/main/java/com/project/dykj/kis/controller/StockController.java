package com.project.dykj.kis.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.project.dykj.kis.model.vo.StockSyncRequest;
import com.project.dykj.kis.model.vo.StockSuggestItem;
import com.project.dykj.kis.model.vo.StockUpsertRequest;
import com.project.dykj.kis.model.vo.VolumeRankItem;
import com.project.dykj.kis.ranking.MarketRankingService;
import com.project.dykj.kis.service.StockService;
import com.project.dykj.kis.service.StockService.MstImportResult;

import lombok.val;

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
	 * 메인페이지용 거래량 TOP10 조회
	 */
	@GetMapping("/top10")
	public List<VolumeRankItem> volumeTop10() {
		return marketRankingService.getVolumeTop10();
	}

	/**
	 * 종목 자동완성
	 */
	@GetMapping("/suggest")
	public List<StockSuggestItem> suggest(
			@RequestParam String q,
			@RequestParam(defaultValue = "10") int limit
	) {
		return stockService.suggest(q, limit);
	}

	/*
	* 검색시 페이지
	* 
	*/
	public void detail(String keyword){

	}

	/**
	 * STOCKS 마스터를 직접 upsert
	 */
	@PostMapping("/import")
	public ResponseEntity<Map<String, Object>> importStocks(@RequestBody List<StockUpsertRequest> items) {
		stockService.upsertStocks(items);
		return ResponseEntity.ok(Map.of("count", items == null ? 0 : items.size()));
	}

	/**
	 * KIS API로 종목정보를 조회한 뒤 STOCKS에 반영
	 */
	@PostMapping("/sync")
	public ResponseEntity<Map<String, Object>> syncFromKis(@RequestBody StockSyncRequest req) {
		List<String> ids = req == null ? List.of() : req.getStockIds();
		String prdtTypeCd = req == null ? null : req.getPrdtTypeCd();
		stockService.syncFromKis(ids, prdtTypeCd);
		return ResponseEntity.ok(Map.of("count", ids == null ? 0 : ids.size()));
	}

	/**
	 * KOSPI 코드 MST 파일을 업로드 받아 STOCKS에 초기 적재
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
