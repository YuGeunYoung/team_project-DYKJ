package com.project.dykj.kis.service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.ArrayList;
import java.util.regex.Pattern;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.kis.model.vo.KisDailyChartResponse;
import com.project.dykj.kis.model.vo.KisStockInfoResponse;
import com.project.dykj.kis.model.vo.StockSearchItem;
import com.project.dykj.kis.model.vo.StockSuggestItem;
import com.project.dykj.kis.model.vo.StockUpsertRequest;

@Service
public class StockService {

	/**
	 * STOCKS(종목 마스터) DB + KIS 시세/정보를 연결하는 서비스.
	 *
	 * 이 클래스의 역할
	 * 1) DB(STOCKS) 기반 기능
	 *   - 자동완성(suggest): 이름/코드 prefix 검색 (가볍고 빠르게)
	 *   - 검색(search): 이름/코드 contains 검색 + 페이지네이션 (검색결과 페이지용)
	 *   - master 조회(findById): 상세페이지에서 종목 고정정보(코드/이름/섹터 등)
	 *
	 * 2) KIS 기반 기능
	 *   - 현재가(getCurrentPrice): KIS inquire-price 호출
	 *   - 차트(getDailyChart): KIS 일/주/월 차트 호출
	 *   - 복수 현재가(getMultiplePrices): 검색결과 리스트에서 최대 20개 종목을 한 번에 조회
	 *
	 * 3) 데이터 적재/동기화
	 *   - KIS 기본정보 sync(syncFromKis): API로 종목 기본정보를 조회해 STOCKS upsert
	 *
	 * 주의사항
	 * - KIS는 초당 제한이 있어 검색결과에서 종목마다 /price를 N번 호출하면 금방 막힘
	 *   → 그래서 /prices(복수 현재가)로 묶어서 호출하도록 설계
	 */
	private static final Pattern STOCK_CODE_PATTERN = Pattern.compile("^(?:A)?\\d{6}$");

	private static final String NS_STOCK = "stockMapper.";

	private final SqlSessionTemplate sqlSession;
	private final KisService kisService;

	public StockService(SqlSessionTemplate sqlSession, KisService kisService) {
		this.sqlSession = sqlSession;
		this.kisService = kisService;
	}

	@Transactional(readOnly = true)
	public StockUpsertRequest findById(String stockId) {
		// DB(STOCKS)에서 종목 마스터(고정 정보) 단건 조회
		String id = stockId == null ? "" : stockId.trim();
		if (id.isEmpty()) {
			throw new IllegalArgumentException("stockId is required");
		}
		return sqlSession.selectOne(NS_STOCK + "selectById", Map.of("stockId", id));
	}

	@Transactional(readOnly = true)
	public Map<?, ?> getCurrentPrice(String stockId) {
		// KIS 현재가 단건 조회 (실시간/변동 데이터)
		String id = stockId == null ? "" : stockId.trim();
		if (id.isEmpty()) {
			throw new IllegalArgumentException("stockId is required");
		}
		return kisService.getStockPrice(id);
	}

	@Transactional(readOnly = true)
	public KisDailyChartResponse getDailyChart(String stockId, String start, String end, String periodDivCode) {
		// KIS 차트 데이터 조회 (그래프용)
		String id = stockId == null ? "" : stockId.trim();
		if (id.isEmpty()) {
			throw new IllegalArgumentException("stockId is required");
		}
		return kisService.fetchDailyChart(id, start, end, periodDivCode);
	}

	/**
	 * Multiple prices for list pages (max 20 codes)
	 */
	@Transactional(readOnly = true)
	public Map<String, Object> getMultiplePrices(List<String> stockIds) {
		// 검색결과 리스트에서 "최대 20개"만 복수현재가로 묶어서 조회
		// (KIS 초당 제한/동시호출 문제를 줄이기 위함)
		if (stockIds == null || stockIds.isEmpty()) {
			return Map.of();
		}
		List<String> codes = stockIds.stream()
				.filter(v -> v != null && !v.trim().isEmpty())
				.map(String::trim)
				.distinct()
				.limit(20)
				.toList();
		if (codes.isEmpty()) {
			return Map.of();
		}

		Map<?, ?> raw = kisService.fetchMultiplePrices(codes);
		return normalizeMultiplePricesResponse(raw);
	}

	private Map<String, Object> normalizeMultiplePricesResponse(Map<?, ?> raw) {
		// KIS 복수현재가 응답(JSON)을 프론트에서 쓰기 쉬운 형태로 정규화
		// - 반환 형태: { "005930": { stck_prpr, prdy_vrss, prdy_ctrt, prdy_vrss_sign }, ... }
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
					item.get("inter_shrn_iscd"),   // intstock-multprice
					item.get("stck_shrn_iscd"),    // other endpoints
					item.get("mksc_shrn_iscd"),    // other endpoints
					item.get("pdno"),              // stock-info
					item.get("STOCK_ID"),
					item.get("stockId"),
					item.get("stock_id")
			);
			stockId = normalizeStockId(stockId);
			if (stockId == null || stockId.isBlank()) {
				continue;
			}

			Map<String, Object> priceInfo = new HashMap<>();
			// normalize to keys used by UI
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
		// KIS 응답마다 output/output1/... 구조가 달라질 수 있어 최대한 안전하게 수집
		List<Map<?, ?>> items = new ArrayList<>();

		Object out = raw.get("output");
		addOutput(items, out);

		// Some KIS endpoints use output1/output2... or other output groups
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
		// output이 Map(단건) 또는 List(Map...)(다건)일 수 있어 둘 다 처리
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
		// 여러 후보 필드 중 값이 존재하는 첫 번째 값을 선택 (응답 필드명 차이 흡수)
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
	 * Upsert into STOCKS (admin/manual import)
	 */
	@Transactional
	public void upsertStocks(List<StockUpsertRequest> items) {
		// 외부에서 StockUpsertRequest 리스트를 받아 STOCKS에 MERGE(upsert)
		if (items == null || items.isEmpty()) {
			return;
		}
		for (StockUpsertRequest req : items) {
			validateUpsert(req);
			sqlSession.update(NS_STOCK + "mergeStock", Map.of("req", req));
		}
	}

	/**
	 * Stock autocomplete (prefix search by name/code)
	 */
	@Transactional(readOnly = true)
	public List<StockSuggestItem> suggest(String q, int limit) {
		// 자동완성: 사용자가 타이핑 중 반복 호출되므로 prefix 검색 + limit 작게
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
	 * Stock search (contains match by name/code) for search result page
	 */
	@Transactional(readOnly = true)
	public List<StockSearchItem> search(String q, int page, int size) {
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
		return sqlSession.selectList(NS_STOCK + "search", params);
	}

	/**
	 * Sync STOCKS from KIS "search-stock-info" API
	 * - stockIds: stock codes list (PDNO)
	 * - prdtTypeCd: product type code (PRDT_TYPE_CD)
	 */
	@Transactional
	public void syncFromKis(List<String> stockIds, String prdtTypeCd) {
		// KIS API로 종목 기본정보를 조회해 STOCKS에 upsert
		if (stockIds == null || stockIds.isEmpty()) {
			return;
		}
		for (String stockId : stockIds) {
			String safeId = stockId == null ? "" : stockId.trim();
			if (safeId.isEmpty()) {
				continue;
			}
			KisStockInfoResponse info = kisService.fetchStockInfo(prdtTypeCd, safeId);
			if (info.getOutput() == null) {
				continue;
			}

			String sector = pickSector(info.getOutput().getStdIdstClsfCdName());
			String isActive = toIsActive(info.getOutput().getTrStopYn(), info.getOutput().getLstgAbolDt());

			StockUpsertRequest req = new StockUpsertRequest(
					info.getOutput().getPdno(),
					info.getOutput().getPrdtName(),
					sector,
					null,
					isActive
			);
			validateUpsert(req);
			sqlSession.update(NS_STOCK + "mergeStock", Map.of("req", req));
		}
	}

	private void validateUpsert(StockUpsertRequest req) {
		// STOCKS에 넣을 데이터 최소 검증 (코드/이름 필수, isActive는 Y/N만 허용)
		if (req == null) {
			throw new IllegalArgumentException("body is required");
		}
		if (isBlank(req.getStockId()) || isBlank(req.getStockName())) {
			throw new IllegalArgumentException("stockId/stockName are required");
		}
		if (req.getIsActive() != null && !req.getIsActive().isBlank()) {
			String v = req.getIsActive().trim();
			if (!"Y".equalsIgnoreCase(v) && !"N".equalsIgnoreCase(v)) {
				throw new IllegalArgumentException("isActive must be Y or N");
			}
		}
	}

	private static String pickSector(String v) {
		// sector(업종/섹터)는 비어있으면 UNKNOWN으로 대체
		String s = v == null ? "" : v.trim();
		return s.isEmpty() ? "UNKNOWN" : s;
	}

	private static String toIsActive(String trStopYn, String lstgAbolDt) {
		// 거래정지/상장폐지 여부로 거래가능여부(Y/N) 판단
		if ("Y".equalsIgnoreCase(trStopYn)) {
			return "N";
		}
		if (lstgAbolDt != null && !lstgAbolDt.trim().isEmpty()) {
			return "N";
		}
		return "Y";
	}

	private static boolean isBlank(String v) {
		return v == null || v.trim().isEmpty();
	}

	private static String normalizeStockId(String rawCode) {
		// MST/입력값에서 종목코드를 6자리로 정규화
		// - "A005930"처럼 앞에 A가 붙으면 제거
		// - 6자리 아니면 null 처리
		if (rawCode == null) {
			return null;
		}
		String c = rawCode.trim();
		if (c.isEmpty()) {
			return null;
		}
		if (!STOCK_CODE_PATTERN.matcher(c).matches()) {
			return null;
		}
		if (c.length() == 7 && (c.startsWith("A") || c.startsWith("a"))) {
			return c.substring(1);
		}
		return c;
	}
}

