package com.project.dykj.kis.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.game.service.GameService;
import com.project.dykj.kis.model.vo.KisDailyChartResponse;
import com.project.dykj.kis.model.vo.StockSearchItem;
import com.project.dykj.kis.model.vo.StockSuggestItem;
import com.project.dykj.kis.model.vo.StockUpsertRequest;

@Service
public class StockService {

	/**
	 * STOCKS(DB)? KIS API瑜??곌껐?섎뒗 ?쒕퉬?ㅼ엯?덈떎.
	 * - ?먮룞?꾩꽦/寃??留덉뒪??議고쉶
	 * - ?④굔 ?꾩옱媛/?쇰큺 李⑦듃 議고쉶
	 * - 蹂듭닔 ?꾩옱媛 議고쉶(由ъ뒪???붾㈃ 理쒖쟻??
	 */
	private static final Pattern STOCK_CODE_PATTERN = Pattern.compile("^(?:A)?\\d{6}$");

	private static final String NS_STOCK = "stockMapper.";

	private final SqlSessionTemplate sqlSession;
	private final KisService kisService;
	private final GameService gameService; //[taek]: ?꾩쟾怨쇱젣 ?ъ꽦 ?뺤씤??

	public StockService(SqlSessionTemplate sqlSession, KisService kisService, GameService gameService) {
		this.sqlSession = sqlSession;
		this.kisService = kisService;
		this.gameService = gameService;
	}

	@Transactional(readOnly = true)
	public StockUpsertRequest findById(String stockId) {
		String id = stockId == null ? "" : stockId.trim();
		if (id.isEmpty()) {
			throw new IllegalArgumentException("stockId is required");
		}
		return sqlSession.selectOne(NS_STOCK + "selectById", Map.of("stockId", id));
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
	 * 由ъ뒪???붾㈃??蹂듭닔 ?꾩옱媛 議고쉶
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
			String stockId = firstNonBlank(
					item.get("inter_shrn_iscd"),
					item.get("stck_shrn_iscd"),
					item.get("mksc_shrn_iscd"),
					item.get("pdno"),
					item.get("STOCK_ID"),
					item.get("stockId"),
					item.get("stock_id")
			);

			Map<String, Object> priceInfo = new HashMap<>();
			priceInfo.put("stck_prpr", firstNonBlank(item.get("stck_prpr"), item.get("inter2_prpr")));
			priceInfo.put("prdy_vrss", firstNonBlank(item.get("prdy_vrss"), item.get("inter2_prdy_vrss")));
			priceInfo.put("prdy_ctrt", firstNonBlank(item.get("prdy_ctrt")));
			priceInfo.put("prdy_vrss_sign", firstNonBlank(item.get("prdy_vrss_sign")));
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

	private static String firstNonBlank(Object... candidates) {
		if (candidates == null) {
			return null;
		}
		for (Object c : candidates) {
			if (c == null) {
				continue;
			}
			String s = String.valueOf(c).trim();
			if (!s.isEmpty()) {
				return s;
			}
		}
		return null;
	}

	/**
	 * ?먮룞?꾩꽦(prefix 寃??
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
	 * 寃??寃곌낵(contains 寃??
	 */
	@Transactional(readOnly = true)
	public List<StockSearchItem> search(String q, int page, int size, String userId) {
		// 寃?됯껐怨??섏씠吏: contains 寃??+ ?섏씠吏?ㅼ씠??
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


