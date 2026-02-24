package com.project.dykj.kis.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.game.service.GameService;
import com.project.dykj.kis.model.vo.KisDailyChartResponse;
import com.project.dykj.kis.model.vo.StockSearchItem;
import com.project.dykj.kis.model.vo.StockSuggestItem;
import com.project.dykj.kis.util.KisValueUtils;

@Service
public class StockService {

	/**
	 * STOCKS(DB)와 KIS API를 연결하는 서비스입니다.
	 * - 자동완성/검색/마스터 조회
	 * - 단건 현재가/일봉 차트 조회
	 * - 복수 현재가 조회(리스트 화면 최적화)
	 */
	private static final String NS_STOCK = "stockMapper.";

	private final SqlSessionTemplate sqlSession;
	private final KisService kisService;
	private final GameService gameService; // [taek]: 이전과제 생성 확인용

	public StockService(SqlSessionTemplate sqlSession, KisService kisService, GameService gameService) {
		this.sqlSession = sqlSession;
		this.kisService = kisService;
		this.gameService = gameService;
	}

	@Transactional(readOnly = true)
	public Map<?, ?> getCurrentPrice(String stockId) {
		String id = stockId == null ? "" : stockId.trim();
		if (id.isEmpty()) {
			throw new IllegalArgumentException("stockId is required");
		}
		return kisService.getStockPrice(id);
	}

	@Transactional(readOnly = true)
	public KisDailyChartResponse getDailyChart(String stockId, String start, String end, String periodDivCode) {
		String id = stockId == null ? "" : stockId.trim();
		if (id.isEmpty()) {
			throw new IllegalArgumentException("stockId is required");
		}
		return kisService.fetchDailyChart(id, start, end, periodDivCode);
	}

	/**
	 * 리스트 화면용 복수 현재가 조회
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> getMultiplePrices(List<String> stockIds) {
		if (stockIds == null || stockIds.isEmpty()) {
			return Map.of();
		}
		List<String> codes = stockIds.stream()
				.filter(v -> v != null && !v.trim().isEmpty())
				.map(String::trim)
				.distinct()
				.limit(30)
				.toList();
		if (codes.isEmpty()) {
			return Map.of();
		}

		Map<?, ?> raw = kisService.fetchMultiplePrices(codes);
		return normalizeMultiplePricesResponse(raw);
	}

	private Map<String, Object> normalizeMultiplePricesResponse(Map<?, ?> raw) {
		if (raw == null) {
			return Map.of();
		}

		Object rtCd = raw.get("rt_cd");
		if (rtCd != null && !"0".equals(String.valueOf(rtCd))) {
			String msgCd = Objects.toString(raw.get("msg_cd"), "");
			String msg1 = Objects.toString(raw.get("msg1"), "");
			throw new IllegalStateException("KIS error: " + msgCd + " - " + msg1);
		}

		List<Map<?, ?>> outputItems = collectOutputItems(raw);
		if (outputItems.isEmpty()) {
			return Map.of();
		}

		Map<String, Object> byId = new HashMap<>();
		for (Map<?, ?> item : outputItems) {
			String stockId = KisValueUtils.firstNonBlank(
					item.get("inter_shrn_iscd"),
					item.get("stck_shrn_iscd"),
					item.get("mksc_shrn_iscd"),
					item.get("pdno"),
					item.get("STOCK_ID"),
					item.get("stockId"),
					item.get("stock_id")
			);

			Map<String, Object> priceInfo = new HashMap<>();
			priceInfo.put("stck_prpr", KisValueUtils.firstNonBlank(item.get("stck_prpr"), item.get("inter2_prpr")));
			priceInfo.put("prdy_vrss", KisValueUtils.firstNonBlank(item.get("prdy_vrss"), item.get("inter2_prdy_vrss")));
			priceInfo.put("prdy_ctrt", KisValueUtils.firstNonBlank(item.get("prdy_ctrt")));
			priceInfo.put("prdy_vrss_sign", KisValueUtils.firstNonBlank(item.get("prdy_vrss_sign")));
			byId.put(stockId, priceInfo);
		}
		return byId;
	}

	@SuppressWarnings("unchecked")
	private static List<Map<?, ?>> collectOutputItems(Map<?, ?> raw) {
		List<Map<?, ?>> items = new ArrayList<>();

		Object out = raw.get("output");
		addOutput(items, out);

		for (Map.Entry<?, ?> e : raw.entrySet()) {
			Object k = e.getKey();
			if (!(k instanceof String key)) {
				continue;
			}
			if ("output".equals(key)) {
				continue;
			}
			if (key.startsWith("output")) {
				addOutput(items, e.getValue());
			}
		}

		return items;
	}

	@SuppressWarnings("unchecked")
	private static void addOutput(List<Map<?, ?>> items, Object output) {
		if (output == null) {
			return;
		}
		if (output instanceof Map<?, ?> map) {
			items.add(map);
			return;
		}
		if (output instanceof List<?> list) {
			for (Object v : list) {
				if (v instanceof Map<?, ?> map) {
					items.add(map);
				}
			}
		}
	}

	/**
	 * 자동완성(prefix 검색)
	 */
	@Transactional(readOnly = true)
	public List<StockSuggestItem> suggest(String q, int limit) {
		String query = q == null ? "" : q.trim();
		if (query.isEmpty()) {
			return List.of();
		}
		int safeLimit = Math.min(20, Math.max(1, limit));
		Map<String, Object> params = new HashMap<>();
		params.put("q", query);
		params.put("limit", safeLimit);
		return sqlSession.selectList(NS_STOCK + "suggest", params);
	}

	/**
	 * 검색 결과(contains 검색)
	 */
	@Transactional(readOnly = true)
	public List<StockSearchItem> search(String q, int page, int size, String userId) {
		// 검색결과 페이지: contains 검색 + 페이지네이션
		String query = q == null ? "" : q.trim();
		if (query.isEmpty()) {
			return List.of();
		}
		int safePage = Math.max(1, page);
		int safeSize = Math.min(50, Math.max(1, size));
		int offset = (safePage - 1) * safeSize;

		Map<String, Object> params = new HashMap<>();
		params.put("q", query);
		params.put("offset", offset);
		params.put("size", safeSize);
		
		if (userId != null) {
			gameService.recordAchievement(userId, 8);
		}
		
		return sqlSession.selectList(NS_STOCK + "search", params);
	}
}


