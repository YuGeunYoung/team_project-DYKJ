package com.project.dykj.kis.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.dykj.kis.model.vo.NaverStockListItem;
import com.project.dykj.kis.model.vo.NaverTradeValueRankItem;

/**
 * 네이버 증권(stock.naver.com) JSON API를 호출해 거래대금 TOP10을 계산하는 서비스입니다.
 * - KIS 데이터와 비교/보완 용도로 사용
 * - 외부 API 호출량을 줄이기 위해 TTL 캐시 사용
 */
@Service
public class NaverRankingService {

	private static final Logger log = LoggerFactory.getLogger(NaverRankingService.class);

	private static final String BASE_URL = "https://stock.naver.com";
	private static final String STOCKLIST_PATH = "/api/domestic/market/stock/default";

	private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(5);
	private static final Duration CACHE_TTL = Duration.ofSeconds(20);

	private final WebClient webClient;
	private final ObjectMapper objectMapper;

	private volatile Cache cache = Cache.empty();

	public NaverRankingService() {
		this.webClient = WebClient.builder()
				.baseUrl(BASE_URL)
				.defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
				.defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
				.build();
		this.objectMapper = new ObjectMapper();
	}

	/**
	 * 네이버 원본(pageSize=100)에서 누적 거래대금(accAmount) 기준 TOP10을 반환합니다.
	 */
	public List<NaverTradeValueRankItem> getTradeValueTop10() {
		Cache cached = this.cache;
		if (cached.isValid()) {
			return cached.tradeValueTop10;
		}

		synchronized (this) {
			cached = this.cache;
			if (cached.isValid()) {
				return cached.tradeValueTop10;
			}

			List<NaverTradeValueRankItem> fresh = fetchTradeValueTop10();
			this.cache = new Cache(fresh, Instant.now().plus(CACHE_TTL));
			return fresh;
		}
	}

	private List<NaverTradeValueRankItem> fetchTradeValueTop10() {
		String uri = UriComponentsBuilder.fromPath(STOCKLIST_PATH)
				.queryParam("tradeType", "KRX")
				.queryParam("marketType", "ALL")
				.queryParam("orderType", "priceTop")
				.queryParam("startIdx", 0)
				.queryParam("pageSize", 100)
				.build(true)
				.toUriString();

		try {
			String json = webClient.get()
					.uri(uri)
					.retrieve()
					.bodyToMono(String.class)
					.block(REQUEST_TIMEOUT);

			if (json == null || json.isBlank()) {
				return List.of();
			}

			List<NaverStockListItem> items = objectMapper.readValue(json, new TypeReference<List<NaverStockListItem>>() {});
			if (items == null || items.isEmpty()) {
				return List.of();
			}

			return items.stream()
					.filter(it -> it.getItemCode() != null && !it.getItemCode().isBlank())
					.sorted(Comparator.comparingLong((NaverStockListItem it) -> parseLongOrMin(it.getAccAmount())).reversed())
					.limit(10)
					.map(it -> new NaverTradeValueRankItem(
							it.getItemCode(),
							it.getItemName(),
							parseLongOrNull(it.getAccAmount()),
							parseLongOrNull(it.getNowVal()),
							parseLongOrNull(it.getChangeVal()),
							parseDoubleOrNull(it.getChangeRate())
					))
					.toList();
		} catch (WebClientResponseException e) {
			String body = e.getResponseBodyAsString();
			if (body != null && body.length() > 2000) {
				body = body.substring(0, 2000) + "...(truncated)";
			}
			log.warn("NAVER API call failed: status={} uri={} body={}", e.getRawStatusCode(), uri, body);
			return List.of();
		} catch (Exception e) {
			log.warn("NAVER API parse failed: uri={} msg={}", uri, e.getMessage());
			return List.of();
		}
	}

	private static long parseLongOrMin(String raw) {
		Long v = parseLongOrNull(raw);
		return v == null ? Long.MIN_VALUE : v;
	}

	private static Long parseLongOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String cleaned = raw.trim().replaceAll("[^0-9\\-]", "");
		if (cleaned.isBlank() || "-".equals(cleaned)) {
			return null;
		}
		try {
			return Long.parseLong(cleaned);
		} catch (Exception e) {
			return null;
		}
	}

	private static Double parseDoubleOrNull(String raw) {
		if (raw == null) {
			return null;
		}
		String cleaned = raw.trim().replaceAll("[^0-9\\-\\.]", "");
		if (cleaned.isBlank() || "-".equals(cleaned)) {
			return null;
		}
		try {
			return Double.parseDouble(cleaned);
		} catch (Exception e) {
			return null;
		}
	}

	private record Cache(List<NaverTradeValueRankItem> tradeValueTop10, Instant expiresAt) {
		static Cache empty() {
			return new Cache(List.of(), Instant.EPOCH);
		}

		boolean isValid() {
			return expiresAt != null && Instant.now().isBefore(expiresAt);
		}
	}
}
