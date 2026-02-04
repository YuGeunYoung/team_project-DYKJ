package com.project.dykj.domain.stock.service;

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
import com.project.dykj.domain.stock.dto.res.NaverStockListItem;
import com.project.dykj.domain.stock.dto.res.NaverTradeValueRankItem;

/**
 * 네이버 증권(stock.naver.com) 내부 JSON API를 호출해서 랭킹 데이터를 만든다.
 *
 * 목적
 * - KIS API로 "시장 전체 거래대금 Top"을 맞추기 어려운 경우(표본/제한/필드) 대안으로 사용
 *
 * 주의
 * - 내부 API는 스펙이 바뀔 수 있음(장애 시 URL/파라미터 재확인 필요)
 * - 프론트 폴링이 잦으면 외부 호출이 폭발하므로 서버에서 캐시(TTL)를 둠
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
	 * 네이버 "priceTop" 표본(pageSize=100)에서 누적 거래대금(accAmount) 기준으로 Top10을 만든다.
	 * - 네이버의 accAmount 집계 기준이 다른 서비스(토스/증권사 HTS)와 다를 수 있어 오차가 발생할 수 있음
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

