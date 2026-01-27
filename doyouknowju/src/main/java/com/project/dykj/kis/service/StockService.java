package com.project.dykj.kis.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.util.List;
import java.util.regex.Pattern;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.kis.model.vo.KisStockInfoResponse;
import com.project.dykj.kis.model.vo.StockSuggestItem;
import com.project.dykj.kis.model.vo.StockUpsertRequest;
import com.project.dykj.kis.mapper.StockMapper;

@Service
public class StockService {

	/**
	 * MST 적재 결과(간단 통계)
	 */
	public record MstImportResult(
			int totalLines,
			int imported,
			int skippedNotStock,
			int skippedInvalid
	) {
	}

	private static final Pattern STOCK_CODE_PATTERN = Pattern.compile("^(?:A)?\\d{6}$");

	private final StockMapper stockMapper;
	private final KisService kisService;

	public StockService(StockMapper stockMapper, KisService kisService) {
		this.stockMapper = stockMapper;
		this.kisService = kisService;
	}

	/**
	 * STOCKS에 직접 upsert(관리/초기 데이터 적재 등)
	 */
	@Transactional
	public void upsertStocks(List<StockUpsertRequest> items) {
		if (items == null || items.isEmpty()) {
			return;
		}
		for (StockUpsertRequest req : items) {
			validateUpsert(req);
			stockMapper.mergeStock(req);
		}
	}

	/**
	 * 종목 자동완성(주식명/종목코드 prefix 검색)
	 */
	@Transactional(readOnly = true)
	public List<StockSuggestItem> suggest(String q, int limit) {
		String query = q == null ? "" : q.trim();
		if (query.isEmpty()) {
			return List.of();
		}
		int safeLimit = Math.min(20, Math.max(1, limit));
		return stockMapper.suggest(query, safeLimit);
	}

	/**
	 * KIS 종목정보(search-stock-info)로 조회한 값을 STOCKS에 반영(upsert)
	 * - stockIds: 종목코드 리스트(PDNO)
	 * - prdtTypeCd: 상품유형코드(PRDT_TYPE_CD)
	 */
	@Transactional
	public void syncFromKis(List<String> stockIds, String prdtTypeCd) {
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
			stockMapper.mergeStock(req);
		}
	}

	/**
	 * KOSPI 코드 MST 파일을 읽어서 STOCKS에 초기 적재(upsert)
	 * - MST는 고정폭(fixed-width) 형식이므로 필요한 구간만 substring으로 파싱
	 * - 현재는 (종목코드/종목명) 중심으로 적재하고, 나머지는 추후 확장 가능
	 */
	@Transactional
	public MstImportResult importKospiCodeMst(InputStream inputStream, Charset charset, boolean onlyStocks) {
		if (inputStream == null) {
			throw new IllegalArgumentException("file is required");
		}
		Charset cs = charset == null ? Charset.forName("MS949") : charset;

		int totalLines = 0;
		int imported = 0;
		int skippedNotStock = 0;
		int skippedInvalid = 0;

		final int codeLen = 9;
		final int isinLen = 12;
		final int nameLen = 40;
		final int nameStart = codeLen + isinLen;

		try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, cs))) {
			String line;
			while ((line = reader.readLine()) != null) {
				totalLines++;

				String raw = line;
				if (raw == null) {
					skippedInvalid++;
					continue;
				}
				if (raw.isBlank()) {
					continue;
				}
				if (raw.length() < nameStart + 1) {
					skippedInvalid++;
					continue;
				}

				String rawCode = safeSubstring(raw, 0, codeLen).trim();
				String stockId = normalizeStockId(rawCode);
				if (stockId == null) {
					if (onlyStocks) {
						skippedNotStock++;
						continue;
					}
					// allow non-stock codes only if caller explicitly permits; still require some id
					skippedNotStock++;
					continue;
				}

				String stockName = safeSubstring(raw, nameStart, nameStart + nameLen).trim();
				if (stockName.isBlank()) {
					skippedInvalid++;
					continue;
				}

				StockUpsertRequest req = new StockUpsertRequest(
						stockId,
						stockName,
						"UNKNOWN",
						null,
						"Y"
				);
				validateUpsert(req);
				stockMapper.mergeStock(req);
				imported++;
			}
		} catch (IOException e) {
			throw new IllegalStateException("failed to read mst file", e);
		}

		return new MstImportResult(totalLines, imported, skippedNotStock, skippedInvalid);
	}

	private void validateUpsert(StockUpsertRequest req) {
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
		String s = v == null ? "" : v.trim();
		return s.isEmpty() ? "UNKNOWN" : s;
	}

	private static String toIsActive(String trStopYn, String lstgAbolDt) {
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

	private static String safeSubstring(String s, int start, int end) {
		if (s == null) {
			return "";
		}
		int safeStart = Math.max(0, Math.min(start, s.length()));
		int safeEnd = Math.max(safeStart, Math.min(end, s.length()));
		return s.substring(safeStart, safeEnd);
	}

	private static String normalizeStockId(String rawCode) {
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
