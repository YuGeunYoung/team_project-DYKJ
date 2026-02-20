package com.project.dykj.kis.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import reactor.netty.http.client.HttpClient;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.util.UriComponentsBuilder;

import com.project.dykj.kis.KisProperties;
import com.project.dykj.kis.model.vo.KisDailyChartResponse;
import com.project.dykj.kis.model.vo.KisRiseFallRankResponse;
import com.project.dykj.kis.model.vo.KisVolumeRankResponse;
import com.project.dykj.kis.model.vo.MarketCapRankItem;
import com.project.dykj.kis.model.vo.RiseFallRankItem;
import com.project.dykj.kis.model.vo.VolumeRankItem;

@Service
public class KisService {

	private static final Logger log = LoggerFactory.getLogger(KisService.class);

	/** KIS API 요청/응답을 처리하는 코어 서비스 */
	private final KisProperties properties;
	private final WebClient webClient;

	private static final Duration RANK_CACHE_TTL = Duration.ofSeconds(10);
	private final Object volumeRankLock = new Object();
	private final Object riseFallRankLock = new Object();
	private final Object marketCapRankLock = new Object();
	private volatile Cache<KisVolumeRankResponse> volumeRankResponseCache = Cache.empty();
	private volatile Cache<List<VolumeRankItem>> volumeTop10Cache = Cache.empty();
	private volatile Cache<List<RiseFallRankItem>> riseRateTop10Cache = Cache.empty();
	private volatile Cache<List<RiseFallRankItem>> fallRateTop10Cache = Cache.empty();
	private volatile Cache<List<MarketCapRankItem>> marketCapTop10Cache = Cache.empty();

	/** 메모리 캐시 Access Token */
	private volatile String accessToken;

	// WebClient(타임아웃 포함) 초기화
	public KisService(KisProperties properties) {
		this.properties = properties;

		HttpClient httpClient = HttpClient.create()
				.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
				.responseTimeout(Duration.ofSeconds(5))
				.doOnConnected(conn -> conn
						.addHandlerLast(new ReadTimeoutHandler(5))
						.addHandlerLast(new WriteTimeoutHandler(5)));

		this.webClient = WebClient.builder()
				.baseUrl(properties.getBaseUrl() == null ? "" : properties.getBaseUrl())
				.clientConnector(new ReactorClientHttpConnector(httpClient))
				.build();
	}

	/** KIS access_token 발급/갱신 (client_credentials) */
	private synchronized void refreshAccessToken() {
		requireBasicConfig();

		Map<String, String> body = new HashMap<>();
		body.put("grant_type", "client_credentials");
		body.put("appkey", properties.getAppkey());
		body.put("appsecret", properties.getAppsecret());

		Map<?, ?> response = webClient.post()
				.uri("/oauth2/tokenP")
				.contentType(MediaType.APPLICATION_JSON)
				.bodyValue(body)
				.retrieve()
				.bodyToMono(Map.class)
				.block();

		if (response != null && response.containsKey("access_token")) {
			this.accessToken = String.valueOf(response.get("access_token"));
		}
	}

	/** accessToken이 없으면 발급 후 반환 */
	private String getValidAccessToken() {
		String token = this.accessToken;
		if (token == null || token.isBlank()) {
			synchronized (this) {
				token = this.accessToken;
				if (token == null || token.isBlank()) {
					refreshAccessToken();
					token = this.accessToken;
				}
			}
		}
		return token;
	}

	/** 거래량 Top10 조회 (TTL 캐시 사용) */
	public List<VolumeRankItem> getVolumeTop10() {
		Cache<List<VolumeRankItem>> cached = this.volumeTop10Cache;
		if (cached.isValid()) return cached.value;

		synchronized (volumeRankLock) {
			cached = this.volumeTop10Cache;
			if (cached.isValid()) return cached.value;

			try {
				KisVolumeRankResponse response = getVolumeRankResponseCached();
				List<KisVolumeRankResponse.OutputItem> output = response.getOutput();
				if (output == null) return List.of();

				List<VolumeRankItem> result = output.stream()
						.sorted(Comparator.comparingInt(it -> parseIntSafe(it.getDataRank())))
						.limit(10)
						.map(it -> new VolumeRankItem(
								it.getMkscShrnIscd(),
								it.getHtsKorIsnm(),
								it.getStckPrpr(),
								it.getPrdyCtrt(),
								it.getAcmlVol()))
						.toList();

				this.volumeTop10Cache = new Cache<>(result, Instant.now().plus(RANK_CACHE_TTL));
				return result;
			} catch (RuntimeException e) {
				if (cached.value != null) return cached.value;
				throw e;
			}
		}
	}

	// 거래량 원본 응답 캐시 조회/갱신 (10초)
	private KisVolumeRankResponse getVolumeRankResponseCached() {
		Cache<KisVolumeRankResponse> cached = this.volumeRankResponseCache;
		if (cached.isValid()) return cached.value;

		try {
			KisVolumeRankResponse response = fetchVolumeRank();
			this.volumeRankResponseCache = new Cache<>(response, Instant.now().plus(RANK_CACHE_TTL));
			return response;
		} catch (RuntimeException e) {
			if (cached.value != null) return cached.value;
			throw e;
		}
	}

	// KIS 거래량 순위 API 원본 응답 조회
	private KisVolumeRankResponse fetchVolumeRank() {
		requireBasicConfig();
		requireApiConfig(
				"kis.volume-rank",
				properties.getVolumeRank() == null ? null : properties.getVolumeRank().getPath(),
				properties.getVolumeRank() == null ? null : properties.getVolumeRank().getTrId()
		);

		String token = requireValidAccessToken();

		KisProperties.Fid fid = properties.getFid();
		String uri = UriComponentsBuilder.fromPath(properties.getVolumeRank().getPath())
				.queryParam("FID_COND_MRKT_DIV_CODE", fid.getCondMrktDivCode())
				.queryParam("FID_COND_SCR_DIV_CODE", fid.getCondScrDivCode())
				.queryParam("FID_INPUT_ISCD", fid.getInputIscd())
				.queryParam("FID_DIV_CLS_CODE", fid.getDivClsCode())
				.queryParam("FID_BLNG_CLS_CODE", fid.getBlngClsCode())
				.queryParam("FID_TRGT_CLS_CODE", fid.getTrgtClsCode())
				.queryParam("FID_TRGT_EXLS_CLS_CODE", fid.getTrgtExlsClsCode())
				.queryParam("FID_INPUT_PRICE_1", fid.getInputPrice1())
				.queryParam("FID_INPUT_PRICE_2", fid.getInputPrice2())
				.queryParam("FID_VOL_CNT", fid.getVolCnt())
				.queryParam("FID_INPUT_DATE_1", fid.getInputDate1())
				.build(true)
				.toUriString();

		KisVolumeRankResponse response;
		try {
			response = prepareGetRequest(uri, token, properties.getVolumeRank().getTrId())
					.retrieve()
					.bodyToMono(KisVolumeRankResponse.class)
					.block(timeout());
		} catch (WebClientResponseException e) {
			logKisError("volume-rank", uri, properties.getVolumeRank().getTrId(), e);
			throw e;
		}

		if (response == null) {
			throw new IllegalStateException("Empty response from KIS volume rank API");
		}
		if (!"0".equals(response.getRtCd())) {
			throw new IllegalStateException("KIS error: " + response.getMsgCd() + " - " + response.getMsg1());
		}
		return response;
	}

	/** 상승률 Top10 조회 */
	public List<RiseFallRankItem> getRiseRateTop10() {
		Cache<List<RiseFallRankItem>> cached = this.riseRateTop10Cache;
		if (cached.isValid()) return cached.value;

		synchronized (riseFallRankLock) {
			cached = this.riseRateTop10Cache;
			if (cached.isValid()) return cached.value;

			try {
				List<RiseFallRankItem> result = fetchRiseFallTop10("0", "1");
				this.riseRateTop10Cache = new Cache<>(result, Instant.now().plus(RANK_CACHE_TTL));
				return result;
			} catch (RuntimeException e) {
				if (cached.value != null) return cached.value;
				throw e;
			}
		}
	}

	/** 하락률 Top10 조회 */
	public List<RiseFallRankItem> getFallRateTop10() {
		Cache<List<RiseFallRankItem>> cached = this.fallRateTop10Cache;
		if (cached.isValid()) return cached.value;

		synchronized (riseFallRankLock) {
			cached = this.fallRateTop10Cache;
			if (cached.isValid()) return cached.value;

			try {
				List<RiseFallRankItem> result = fetchRiseFallTop10("1", "1");
				this.fallRateTop10Cache = new Cache<>(result, Instant.now().plus(RANK_CACHE_TTL));
				return result;
			} catch (RuntimeException e) {
				if (cached.value != null) return cached.value;
				throw e;
			}
		}
	}

	/** 상승/하락 공통 랭킹 조회 */
	private List<RiseFallRankItem> fetchRiseFallTop10(String rankSortClsCode, String prcClsCode) {
		requireBasicConfig();
		requireApiConfig(
				"kis.rise-fall-rank",
				properties.getRiseFallRank() == null ? null : properties.getRiseFallRank().getPath(),
				properties.getRiseFallRank() == null ? null : properties.getRiseFallRank().getTrId()
		);

		String token = requireValidAccessToken();

		String uri = UriComponentsBuilder.fromPath(properties.getRiseFallRank().getPath())
				.queryParam("fid_rsfl_rate2", "")
				.queryParam("fid_cond_mrkt_div_code", properties.getRiseFallRank().getCondMrktDivCode())
				.queryParam("fid_cond_scr_div_code", properties.getRiseFallRank().getCondScrDivCode())
				.queryParam("fid_input_iscd", properties.getRiseFallRank().getInputIscd())
				.queryParam("fid_rank_sort_cls_code", rankSortClsCode)
				.queryParam("fid_input_cnt_1", "0")
				.queryParam("fid_prc_cls_code", prcClsCode)
				.queryParam("fid_input_price_1", "")
				.queryParam("fid_input_price_2", "")
				.queryParam("fid_vol_cnt", "")
				.queryParam("fid_trgt_cls_code", "0")
				.queryParam("fid_trgt_exls_cls_code", "0")
				.queryParam("fid_div_cls_code", "0")
				.queryParam("fid_rsfl_rate1", "")
				.build(true)
				.toUriString();

		try {
			KisRiseFallRankResponse response = prepareGetRequest(uri, token, properties.getRiseFallRank().getTrId())
					.retrieve()
					.bodyToMono(KisRiseFallRankResponse.class)
					.block(timeout());

			if (response == null) {
				return List.of();
			}
			if (!"0".equals(response.getRtCd())) {
				log.warn("KIS rise-fall-rank returned error: msg_cd={} msg1={} uri={}",
						response.getMsgCd(), response.getMsg1(), uri);
				return List.of();
			}

			List<KisRiseFallRankResponse.OutputItem> output = response.getOutput();
			if (output == null) {
				return List.of();
			}

			Comparator<KisRiseFallRankResponse.OutputItem> bySignedRate = Comparator
					.comparingDouble(it -> parseDoubleSafe(normalizeSignedChangeRate(it.getPrdyCtrt(), it.getPrdyVrssSign())));
			if ("0".equals(rankSortClsCode)) {
				bySignedRate = bySignedRate.reversed(); // 상승률 내림차순
			}

			return output.stream()
					.filter(it -> {
						String sign = it.getPrdyVrssSign();
						if ("0".equals(rankSortClsCode)) return isRiseSign(sign);
						if ("1".equals(rankSortClsCode)) return isFallSign(sign);
						return true;
					})
					.sorted(bySignedRate)
					.limit(10)
					.map(it -> new RiseFallRankItem(
							it.getStckShrnIscd(),
							it.getHtsKorIsnm(),
							it.getStckPrpr(),
							it.getPrdyVrssSign(),
							it.getPrdyVrss(),
							normalizeSignedChangeRate(it.getPrdyCtrt(), it.getPrdyVrssSign()),
							it.getAcmlVol()
					))
					.toList();
		} catch (WebClientResponseException e) {
			logKisError("rise-fall-rank", uri, properties.getRiseFallRank().getTrId(), e);
			throw e;
		}
	}

	/** 시가총액 Top10 조회 */
	public List<MarketCapRankItem> getMarketCapTop10() {
		Cache<List<MarketCapRankItem>> cached = this.marketCapTop10Cache;
		if (cached.isValid()) return cached.value;

		synchronized (marketCapRankLock) {
			cached = this.marketCapTop10Cache;
			if (cached.isValid()) return cached.value;

			try {
				List<MarketCapRankItem> result = fetchMarketCapTop10();
				this.marketCapTop10Cache = new Cache<>(result, Instant.now().plus(RANK_CACHE_TTL));
				return result;
			} catch (RuntimeException e) {
				if (cached.value != null) return cached.value;
				throw e;
			}
		}
	}

	// KIS 시가총액 순위 API 조회 후 Top10 반환
	private List<MarketCapRankItem> fetchMarketCapTop10() {
		requireBasicConfig();
		requireApiConfig(
				"kis.market-cap-rank",
				properties.getMarketCapRank() == null ? null : properties.getMarketCapRank().getPath(),
				properties.getMarketCapRank() == null ? null : properties.getMarketCapRank().getTrId()
		);

		String token = requireValidAccessToken();

		String uri = UriComponentsBuilder.fromPath(properties.getMarketCapRank().getPath())
				.queryParam("fid_cond_mrkt_div_code", properties.getMarketCapRank().getCondMrktDivCode())
				.queryParam("fid_cond_scr_div_code", properties.getMarketCapRank().getCondScrDivCode())
				.queryParam("fid_input_iscd", properties.getMarketCapRank().getInputIscd())
				.queryParam("fid_trgt_cls_code", "0")
				.queryParam("fid_trgt_exls_cls_code", "0")
				.queryParam("fid_div_cls_code", "0")
				.queryParam("fid_input_price_1", "")
				.queryParam("fid_input_price_2", "")
				.queryParam("fid_vol_cnt", "")
				.build(true)
				.toUriString();

		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> response = prepareGetRequest(uri, token, properties.getMarketCapRank().getTrId())
					.retrieve()
					.bodyToMono(Map.class)
					.block(timeout());

			if (response == null) {
				return List.of();
			}
			Object rtCd = response.get("rt_cd");
			if (rtCd != null && !"0".equals(String.valueOf(rtCd))) {
				String msgCd = String.valueOf(response.get("msg_cd"));
				String msg1 = String.valueOf(response.get("msg1"));
				log.warn("KIS market-cap-rank returned error: msg_cd={} msg1={} uri={}", msgCd, msg1, uri);
				throw new IllegalStateException("KIS market-cap-rank error: " + msgCd + " - " + msg1);
			}

			Object outputRaw = response.get("output");
			List<?> outputList = (outputRaw instanceof List<?> list) ? list : null;
			if (outputList == null) {
				Object fallback = response.get("output1");
				outputList = (fallback instanceof List<?> list) ? list : null;
			}
			if (outputList == null) {
				return List.of();
			}

			return outputList.stream()
					.filter(v -> v instanceof Map<?, ?>)
					.map(v -> (Map<?, ?>) v)
					.map(it -> {
						String stockId = pick(it, "mksc_shrn_iscd", "stck_shrn_iscd", "inter_shrn_iscd", "jong_code", "itemcode");
						String stockName = pick(it, "hts_kor_isnm", "inter_kor_isnm", "prdt_name", "itemname");
						String currentPrice = pick(it, "stck_prpr", "nowVal");
						String changeSign = pick(it, "prdy_vrss_sign");
						String changeValue = pick(it, "prdy_vrss", "changeVal");
						String changeRate = pick(it, "prdy_ctrt", "changeRate");
						String volume = pick(it, "acml_vol", "accQuant");
						String marketCap = pick(it, "stck_avls", "market_cap", "market_sum", "marketSum");
						String marketShareRate = pick(it, "mrkt_whol_avls_rlim");
						String dataRank = pick(it, "data_rank", "dataRank");
						return new MarketCapRankItem(
								stockId,
								stockName,
								currentPrice,
								changeSign,
								changeValue,
								changeRate,
								volume,
								marketCap,
								marketShareRate,
								dataRank);
					})
					.sorted(Comparator.comparingInt(it -> parseIntSafe(it.getDataRank())))
					.limit(10)
					.toList();
		} catch (WebClientResponseException e) {
			logKisError("market-cap-rank", uri, properties.getMarketCapRank().getTrId(), e);
			throw e;
		}
	}

	// 여러 후보 중 첫 번째 유효 문자열 반환
	private static String pick(Map<?, ?> map, String... keys) {
		if (map == null || keys == null) return null;
		for (String key : keys) {
			if (key == null) continue;
			Object v = map.get(key);
			if (v == null) continue;
			String s = String.valueOf(v).trim();
			if (!s.isEmpty()) return s;
		}
		return null;
	}

	// 단건 현재가 조회
	public Map<?, ?> getStockPrice(String stockCode) {
		requireBasicConfig();

		String token = requireValidAccessToken();

		try {
			return prepareGetRequest(
						UriComponentsBuilder.fromPath("/uapi/domestic-stock/v1/quotations/inquire-price")
								.queryParam("fid_cond_mrkt_div_code", "J")
								.queryParam("fid_input_iscd", stockCode)
								.build(true)
								.toUriString(),
						token,
						"FHKST01010100")
				.retrieve()
				.bodyToMono(Map.class)
				.block(timeout());
		} catch (WebClientResponseException e) {
			logKisError("inquire-price", "/uapi/domestic-stock/v1/quotations/inquire-price", "FHKST01010100", e);
			throw e;
		}
	}

	/** 일봉 차트 조회 (start/end: YYYYMMDD) */
	public KisDailyChartResponse fetchDailyChart(String stockId, String start, String end, String periodDivCode) {
		requireBasicConfig();
		requireApiConfig(
				"kis.daily-chart",
				properties.getDailyChart() == null ? null : properties.getDailyChart().getPath(),
				properties.getDailyChart() == null ? null : properties.getDailyChart().getTrId()
		);

		if (stockId == null || stockId.isBlank()) {
			throw new IllegalArgumentException("stockId is required");
		}

		String token = requireValidAccessToken();

		String safePeriod = (periodDivCode == null || periodDivCode.isBlank())
				? properties.getDailyChart().getPeriodDivCode()
				: periodDivCode;

		String uri = UriComponentsBuilder.fromPath(properties.getDailyChart().getPath())
				.queryParam("FID_COND_MRKT_DIV_CODE", properties.getFid().getCondMrktDivCode())
				.queryParam("FID_INPUT_ISCD", stockId)
				.queryParam("FID_INPUT_DATE_1", start == null ? "" : start)
				.queryParam("FID_INPUT_DATE_2", end == null ? "" : end)
				.queryParam("FID_PERIOD_DIV_CODE", safePeriod)
				.queryParam("FID_ORG_ADJ_PRC", properties.getDailyChart().getOrgAdjPrc())
				.build(true)
				.toUriString();

		KisDailyChartResponse response;
		try {
			response = prepareGetRequest(uri, token, properties.getDailyChart().getTrId())
					.retrieve()
					.bodyToMono(KisDailyChartResponse.class)
					.block(timeout());
		} catch (WebClientResponseException e) {
			logKisError("daily-chart", uri, properties.getDailyChart().getTrId(), e);
			throw e;
		}

		if (response == null) {
			throw new IllegalStateException("Empty response from KIS daily chart API");
		}
		if (!"0".equals(response.getRtCd())) {
			throw new IllegalStateException("KIS error: " + response.getMsgCd() + " - " + response.getMsg1());
		}
		return response;
	}

	/** 복수 종목 현재가 조회 */
	public Map<?, ?> fetchMultiplePrices(List<String> stockIds) {
		requireBasicConfig();
		requireApiConfig(
				"kis.multi-price",
				properties.getMultiPrice() == null ? null : properties.getMultiPrice().getPath(),
				properties.getMultiPrice() == null ? null : properties.getMultiPrice().getTrId()
		);

		if (stockIds == null || stockIds.isEmpty()) {
			throw new IllegalArgumentException("stockIds are required");
		}

		String token = requireValidAccessToken();

		// KIS multi-price endpoint expects numbered params:
		// FID_COND_MRKT_DIV_CODE_1..30, FID_INPUT_ISCD_1..30
		String marketDivCode = properties.getFid().getCondMrktDivCode();
		UriComponentsBuilder builder = UriComponentsBuilder.fromPath(properties.getMultiPrice().getPath());
		for (int i = 0; i < stockIds.size(); i++) {
			int n = i + 1;
			String stockId = stockIds.get(i);
			builder = builder
					.queryParam("FID_COND_MRKT_DIV_CODE_" + n, marketDivCode)
					.queryParam("FID_INPUT_ISCD_" + n, stockId);
		}
		String uri = builder.build(true).toUriString();

		try {
			return prepareGetRequest(uri, token, properties.getMultiPrice().getTrId())
					.retrieve()
					.bodyToMono(Map.class)
					.block(timeout());
		} catch (WebClientResponseException e) {
			logKisError("intstock-multprice", uri, properties.getMultiPrice().getTrId(), e);
			throw e;
		}
	}

	// KIS API 오류 공통 로깅
	private void logKisError(String apiName, String uri, String trId, WebClientResponseException e) {
		String body = e.getResponseBodyAsString();
		if (body != null && body.length() > 2000) {
			body = body.substring(0, 2000) + "...(truncated)";
		}
		log.warn(
				"KIS API call failed: api={} status={} uri={} tr_id={} body={}",
				apiName,
				e.getRawStatusCode(),
				uri,
				trId,
				body
		);
	}

	// GET 요청 공통 헤더(appkey/appsecret/tr_id 등) 세팅
	private WebClient.RequestHeadersSpec<?> prepareGetRequest(String uri, String token, String trId) {
		return webClient.get()
				.uri(uri)
				.accept(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.header("appkey", properties.getAppkey())
				.header("appsecret", properties.getAppsecret())
				.header("tr_id", trId)
				.header("custtype", properties.getCusttype());
	}

	// 토큰이 비어 있으면 예외 발생
	private String requireValidAccessToken() {
		String token = getValidAccessToken();
		if (token == null || token.isBlank()) {
			throw new IllegalStateException("KIS access token is missing");
		}
		return token;
	}

	// KIS 공통 설정(base-url/appkey/appsecret) 검증
	private void requireBasicConfig() {
		if (properties.getBaseUrl() == null || properties.getBaseUrl().isBlank()) {
			throw new IllegalStateException("kis.base-url is required");
		}
		if (properties.getAppkey() == null || properties.getAppkey().isBlank()) {
			throw new IllegalStateException("kis.appkey is required");
		}
		if (properties.getAppsecret() == null || properties.getAppsecret().isBlank()) {
			throw new IllegalStateException("kis.appsecret is required");
		}
	}

	private void requireApiConfig(String configName, String path, String trId) {
		if (path == null || path.isBlank()) {
			throw new IllegalStateException(configName + ".path is required");
		}
		if (trId == null || trId.isBlank()) {
			throw new IllegalStateException(configName + ".tr-id is required");
		}
	}

	// 요청 타임아웃 기본값(5초)
	private Duration timeout() {
		Duration t = properties.getVolumeRank().getTimeout();
		return t == null ? Duration.ofSeconds(5) : t;
	}

	// 문자열 → int 변환(실패 시 MAX_VALUE)
	private static int parseIntSafe(String value) {
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return Integer.MAX_VALUE;
		}
	}

	// 문자열 → double 변환(실패 시 음의 무한대)
	private static double parseDoubleSafe(String value) {
		try {
			if (value == null) return Double.NEGATIVE_INFINITY;
			String cleaned = value.trim();
			if (cleaned.isEmpty() || "-".equals(cleaned)) return Double.NEGATIVE_INFINITY;
			return Double.parseDouble(cleaned);
		} catch (Exception e) {
			return Double.NEGATIVE_INFINITY;
		}
	}

	// 상승 부호 여부 확인
	private static boolean isRiseSign(String signCode) {
		return "1".equals(signCode) || "2".equals(signCode);
	}

	// 하락 부호 여부 확인
	private static boolean isFallSign(String signCode) {
		return "4".equals(signCode) || "5".equals(signCode);
	}

	/** sign 코드 기준으로 등락률 부호를 정규화 */
	private static String normalizeSignedChangeRate(String rawRate, String signCode) {
		if (rawRate == null) return null;
		String cleaned = rawRate.trim();
		if (cleaned.isEmpty() || "-".equals(cleaned)) return rawRate;

		double value;
		try {
			value = Double.parseDouble(cleaned);
		} catch (Exception e) {
			return rawRate;
		}

		double abs = Math.abs(value);
		if (isRiseSign(signCode)) {
			return String.format(Locale.US, "%.2f", abs);
		}
		if (isFallSign(signCode)) {
			return String.format(Locale.US, "%.2f", -abs);
		}
		return rawRate;
	}

	private record Cache<T>(T value, Instant expiresAt) {
		static <T> Cache<T> empty() {
			return new Cache<>(null, Instant.EPOCH);
		}

		boolean isValid() {
			return value != null && expiresAt != null && Instant.now().isBefore(expiresAt);
		}
	}
}

