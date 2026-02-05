package com.project.dykj.kis.service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
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
import com.project.dykj.kis.model.vo.KisStockInfoResponse;
import com.project.dykj.kis.model.vo.KisVolumeRankResponse;
import com.project.dykj.kis.model.vo.MarketCapRankItem;
import com.project.dykj.kis.model.vo.RiseFallRankItem;
import com.project.dykj.kis.model.vo.TradeAmountRankItem;
import com.project.dykj.kis.model.vo.VolumeRankItem;

@Service
public class KisService {

	private static final Logger log = LoggerFactory.getLogger(KisService.class);

	/**
	 * KIS(한국투자증권) OpenAPI 호출 전용 서비스.
	 *
	 * 이 클래스의 역할
	 * - access token 발급/보관(메모리) 및 필요 시 자동 발급
	 * - KIS REST API 호출(WebClient)과 요청 헤더(tr_id/appkey/appsecret/custtype) 구성
	 * - 실패 시(KIS 4xx/5xx) 응답 바디를 포함해 로그로 남겨 원인 분석 가능하게 함
	 *
	 * 주의사항
	 * - KIS는 초당 호출 제한이 있어(예: EGW00201) 과도한 폴링/동시호출은 500으로 실패할 수 있음
	 * - 일부 API는 환경(모의/실전)에 따라 baseUrl/tr_id가 달라짐
	 */
	private final KisProperties properties;
	private final WebClient webClient;

	// KIS는 초당 호출 제한이 있어(예: EGW00201) 프론트에서 폴링/중복 호출이 발생하면 500이 자주 납니다.
	// 메인 화면용 랭킹 API는 짧게 캐시해서 외부 호출을 줄입니다.
	private static final Duration RANK_CACHE_TTL = Duration.ofSeconds(10);
	private final Object volumeRankLock = new Object();
	private final Object riseFallRankLock = new Object();
	private final Object marketCapRankLock = new Object();
	private volatile Cache<KisVolumeRankResponse> volumeRankResponseCache = Cache.empty();
	private volatile Cache<List<VolumeRankItem>> volumeTop10Cache = Cache.empty();
	private volatile Cache<List<TradeAmountRankItem>> tradeAmountTop10Cache = Cache.empty();
	private volatile Cache<List<RiseFallRankItem>> riseRateTop10Cache = Cache.empty();
	private volatile Cache<List<RiseFallRankItem>> fallRateTop10Cache = Cache.empty();
	private volatile Cache<List<MarketCapRankItem>> marketCapTop10Cache = Cache.empty();

	/**
	 * access token은 서버 메모리에만 저장(재시작하면 초기화).
	 * - 장기적으로는 스케줄러로 24시간 갱신/재발급 관리 권장
	 */
	private volatile String accessToken;

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

	/**
	 * KIS access_token 발급/갱신 (client_credentials)
	 * - KIS는 대부분의 시세/정보 API 호출 전에 Bearer 토큰이 필요함
	 * - 이 메소드는 토큰을 받아 accessToken 필드에 저장
	 */
	public synchronized void refreshAccessToken() {
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

	/**
	 * accessToken이 없으면 자동으로 발급 후 반환
	 * - 최초 호출 시 refreshAccessToken()을 실행
	 */
	public String getValidAccessToken() {
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

	/**
	 * 거래량 랭킹 응답을 Top10 형태로 변환해서 반환
	 * - 내부적으로 volume-rank API 호출 후, data_rank 기준 정렬 + 상위 10개만 매핑
	 * - 프론트 메인페이지 등에서 사용
	 */
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
				// 레이트리밋 등으로 실패해도 마지막(만료된 캐시 포함) 값이 있으면 화면은 유지
				if (cached.value != null) return cached.value;
				throw e;
			}
		}
	}

	/**
	 * 거래대금 Top10 (거래량 순위 API의 표본 30건 내에서 재정렬)
	 * - volume-rank 응답(output)에는 누적 거래대금(acml_tr_pbmn)이 포함되어 있어 이를 기준으로 내림차순 정렬
	 * - 단, volume-rank 자체가 최대 30건만 제공하므로 "시장 전체 거래대금 Top10"과는 오차가 있을 수 있음
	 */
	public List<TradeAmountRankItem> getTradeAmountTop10() {
		Cache<List<TradeAmountRankItem>> cached = this.tradeAmountTop10Cache;
		if (cached.isValid()) return cached.value;

		synchronized (volumeRankLock) {
			cached = this.tradeAmountTop10Cache;
			if (cached.isValid()) return cached.value;

			try {
				KisVolumeRankResponse response = getVolumeRankResponseCached();
				List<KisVolumeRankResponse.OutputItem> output = response.getOutput();
				if (output == null) return List.of();

				List<TradeAmountRankItem> result = output.stream()
						.sorted(Comparator.comparingLong((KisVolumeRankResponse.OutputItem it) -> parseLongSafe(it.getAcmlTrPbmn())).reversed())
						.limit(10)
						.map(it -> new TradeAmountRankItem(
								it.getMkscShrnIscd(),
								it.getHtsKorIsnm(),
								it.getStckPrpr(),
								it.getPrdyCtrt(),
								it.getAcmlVol(),
								it.getAcmlTrPbmn()))
						.toList();

				this.tradeAmountTop10Cache = new Cache<>(result, Instant.now().plus(RANK_CACHE_TTL));
				return result;
			} catch (RuntimeException e) {
				if (cached.value != null) return cached.value;
				throw e;
			}
		}
	}

	/**
	 * 거래량 랭킹 원본 응답 조회 (필요 시 확장용)
	 * - properties.kis.volume-rank.path / tr-id / fid 기본값이 필요
	 * - KIS 서버에서 오류코드(rt_cd != 0)면 IllegalStateException 발생
	 */
	/**
	 * 등락률(상승률) Top10
	 * - KIS 등락률 순위 API를 호출해 상승률 순(fid_rank_sort_cls_code=0)으로 상위 10개를 반환
	 */
	public List<RiseFallRankItem> getRiseRateTop10() {
		Cache<List<RiseFallRankItem>> cached = this.riseRateTop10Cache;
		if (cached.isValid()) return cached.value;

		synchronized (riseFallRankLock) {
			cached = this.riseRateTop10Cache;
			if (cached.isValid()) return cached.value;

			try {
				// 상승률순(fid_rank_sort_cls_code=0) + 종가대비(fid_prc_cls_code=1)
				List<RiseFallRankItem> result = fetchRiseFallTop10("0", "1");
				this.riseRateTop10Cache = new Cache<>(result, Instant.now().plus(RANK_CACHE_TTL));
				return result;
			} catch (RuntimeException e) {
				if (cached.value != null) return cached.value;
				throw e;
			}
		}
	}

	/**
	 * 등락률(하락률) Top10
	 * - KIS 등락률 순위 API를 호출해 하락률 순(fid_rank_sort_cls_code=1)으로 상위 10개를 반환
	 */
	public List<RiseFallRankItem> getFallRateTop10() {
		Cache<List<RiseFallRankItem>> cached = this.fallRateTop10Cache;
		if (cached.isValid()) return cached.value;

		synchronized (riseFallRankLock) {
			cached = this.fallRateTop10Cache;
			if (cached.isValid()) return cached.value;

			try {
				// 하락률순(fid_rank_sort_cls_code=1) + 종가대비(fid_prc_cls_code=1)
				List<RiseFallRankItem> result = fetchRiseFallTop10("1", "1");
				this.fallRateTop10Cache = new Cache<>(result, Instant.now().plus(RANK_CACHE_TTL));
				return result;
			} catch (RuntimeException e) {
				if (cached.value != null) return cached.value;
				throw e;
			}
		}
	}


/**
 * 거래량순위(volume-rank) 원본 응답을 짧게 캐시합니다.
 * - /top10 과 /top10/trade-amount 가 동시에 호출되면 KIS를 2번 치게 되는데,
 *   초당 제한(EGW00201)에 걸려 500이 발생할 수 있어 1번 호출 결과를 공유합니다.
 */
	/**
	 * 시가총액 Top10
	 * - KIS 시가총액 순위 API를 호출해 상위 10개를 반환합니다.
	 */
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

private KisVolumeRankResponse getVolumeRankResponseCached() {
	Cache<KisVolumeRankResponse> cached = this.volumeRankResponseCache;
	if (cached.isValid()) return cached.value;

	try {
		KisVolumeRankResponse response = fetchVolumeRank();
		this.volumeRankResponseCache = new Cache<>(response, Instant.now().plus(RANK_CACHE_TTL));
		return response;
	} catch (RuntimeException e) {
		// 만료된 캐시라도 값이 있으면 그것을 반환(화면 깨짐 방지)
		if (cached.value != null) return cached.value;
		throw e;
	}
}

	public KisVolumeRankResponse fetchVolumeRank() {
		requireBasicConfig();
		requireVolumeRankConfig();

		String token = getValidAccessToken();
		if (token == null || token.isBlank()) {
			throw new IllegalStateException("KIS access token is missing");
		}

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
			response = webClient.get()
					.uri(uri)
					.accept(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.header("appkey", properties.getAppkey())
					.header("appsecret", properties.getAppsecret())
					.header("tr_id", properties.getVolumeRank().getTrId())
					.header("custtype", properties.getCusttype())
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

	/**
	 * 현재가 조회 (inquire-price)
	 * - 반환은 Map으로 두었고, 필요하면 VO로 모델링 가능
	 * - 프론트에서 "현재가/등락률" 등을 표시할 때 사용
	 */
	private List<RiseFallRankItem> fetchRiseFallTop10(String rankSortClsCode, String prcClsCode) {
		requireBasicConfig();
		requireRiseFallRankConfig();

		String token = getValidAccessToken();
		if (token == null || token.isBlank()) {
			throw new IllegalStateException("KIS access token is missing");
		}

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
			KisRiseFallRankResponse response = webClient.get()
					.uri(uri)
					.accept(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.header("appkey", properties.getAppkey())
					.header("appsecret", properties.getAppsecret())
					.header("tr_id", properties.getRiseFallRank().getTrId())
					.header("custtype", properties.getCusttype())
					.retrieve()
					.bodyToMono(KisRiseFallRankResponse.class)
					.block(Duration.ofSeconds(5));

			if (response == null) {
				return List.of();
			}
			if (!"0".equals(response.getRtCd())) {
				log.warn("KIS rise-fall-rank returned error: msg_cd={} msg1={} uri={}",
						response.getMsgCd(), response.getMsg1(), uri);
				return List.of();
			}

			List<KisRiseFallRankResponse.OutputItem> output = response == null ? null : response.getOutput();
			if (output == null) {
				return List.of();
			}

			// KIS의 data_rank가 기대와 다르게 보이는 경우가 있어,
			// 프론트 요구사항(종가대비 등락률) 기준으로 서버에서 한 번 더 정렬합니다.
			Comparator<KisRiseFallRankResponse.OutputItem> byChangeRate = Comparator
					.comparingDouble(it -> parseDoubleSafe(it.getPrdyCtrt()));
			if ("0".equals(rankSortClsCode)) {
				byChangeRate = byChangeRate.reversed(); // 상승률 Top
			}

			return output.stream()
					.sorted(byChangeRate)
					.limit(10)
					.map(it -> new RiseFallRankItem(
							it.getStckShrnIscd(),
							it.getHtsKorIsnm(),
							it.getStckPrpr(),
							it.getPrdyVrssSign(),
							it.getPrdyVrss(),
							it.getPrdyCtrt(),
							it.getAcmlVol()
					))
					.toList();
		} catch (WebClientResponseException e) {
			logKisError("rise-fall-rank", uri, properties.getRiseFallRank().getTrId(), e);
			throw e;
		}
	}

	private List<MarketCapRankItem> fetchMarketCapTop10() {
		requireBasicConfig();
		requireMarketCapRankConfig();

		String token = getValidAccessToken();
		if (token == null || token.isBlank()) {
			throw new IllegalStateException("KIS access token is missing");
		}

		String uri = UriComponentsBuilder.fromPath(properties.getMarketCapRank().getPath())
				.queryParam("fid_cond_mrkt_div_code", properties.getMarketCapRank().getCondMrktDivCode())
				.queryParam("fid_cond_scr_div_code", properties.getMarketCapRank().getCondScrDivCode())
				.queryParam("fid_input_iscd", properties.getMarketCapRank().getInputIscd())
				.queryParam("fid_trgt_cls_code", "0")
				.queryParam("fid_trgt_exls_cls_code", "0")
				.queryParam("fid_div_cls_code", "0")
				.build(true)
				.toUriString();

		try {
			@SuppressWarnings("unchecked")
			Map<String, Object> response = webClient.get()
					.uri(uri)
					.accept(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.header("appkey", properties.getAppkey())
					.header("appsecret", properties.getAppsecret())
					.header("tr_id", properties.getMarketCapRank().getTrId())
					.header("custtype", properties.getCusttype())
					.retrieve()
					.bodyToMono(Map.class)
					.block(Duration.ofSeconds(5));

			if (response == null) {
				return List.of();
			}
			Object rtCd = response.get("rt_cd");
			if (rtCd != null && !"0".equals(String.valueOf(rtCd))) {
				log.warn("KIS market-cap-rank returned error: msg_cd={} msg1={} uri={}",
						response.get("msg_cd"), response.get("msg1"), uri);
				return List.of();
			}

			Object outputRaw = response.get("output");
			List<?> outputList = (outputRaw instanceof List<?> list) ? list : null;
			if (outputList == null) {
				// 일부 API는 output1/output2 형태일 수 있어 fallback
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
						String stockId = pick(it, "stck_shrn_iscd", "mksc_shrn_iscd", "inter_shrn_iscd", "jong_code", "itemcode");
						String stockName = pick(it, "hts_kor_isnm", "inter_kor_isnm", "prdt_name", "itemname");
						String currentPrice = pick(it, "stck_prpr", "nowVal");
						String changeSign = pick(it, "prdy_vrss_sign");
						String changeValue = pick(it, "prdy_vrss", "changeVal");
						String changeRate = pick(it, "prdy_ctrt", "changeRate");
						String marketCap = pick(it, "stck_avls", "market_cap", "market_sum", "marketSum");
						String dataRank = pick(it, "data_rank", "dataRank");
						return new MarketCapRankItem(stockId, stockName, currentPrice, changeSign, changeValue, changeRate, marketCap, dataRank);
					})
					.sorted(Comparator.comparingInt(it -> parseIntSafe(it.getDataRank())))
					.limit(10)
					.toList();
		} catch (WebClientResponseException e) {
			logKisError("market-cap-rank", uri, properties.getMarketCapRank().getTrId(), e);
			throw e;
		}
	}

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

	public Map<?, ?> getStockPrice(String stockCode) {
		requireBasicConfig();

		String token = getValidAccessToken();
		if (token == null || token.isBlank()) {
			throw new IllegalStateException("KIS access token is missing");
		}

		try {
			return webClient.get()
				.uri(uriBuilder -> uriBuilder
						.path("/uapi/domestic-stock/v1/quotations/inquire-price")
						.queryParam("fid_cond_mrkt_div_code", "J")
						.queryParam("fid_input_iscd", stockCode)
						.build())
				.accept(MediaType.APPLICATION_JSON)
				.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
				.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
				.header("appkey", properties.getAppkey())
				.header("appsecret", properties.getAppsecret())
				.header("tr_id", "FHKST01010100")
				.header("custtype", properties.getCusttype())
				.retrieve()
				.bodyToMono(Map.class)
				.block();
		} catch (WebClientResponseException e) {
			logKisError("inquire-price", "/uapi/domestic-stock/v1/quotations/inquire-price", "FHKST01010100", e);
			throw e;
		}
	}

	/**
	 * 종목 기본정보 조회 (search-stock-info)
	 * - 종목코드(PDNO) 단건으로 기본정보를 가져오는 용도
	 * - DB(STOCKS) 동기화(sync)할 때 사용 가능
	 */
	public KisStockInfoResponse fetchStockInfo(String prdtTypeCd, String pdno) {
		requireBasicConfig();
		requireStockInfoConfig();

		if (pdno == null || pdno.isBlank()) {
			throw new IllegalArgumentException("pdno(stock id) is required");
		}
		String safePrdtTypeCd = (prdtTypeCd == null || prdtTypeCd.isBlank())
				? properties.getStockInfo().getPrdtTypeCd()
				: prdtTypeCd;
		if (safePrdtTypeCd == null || safePrdtTypeCd.isBlank()) {
			throw new IllegalArgumentException("prdtTypeCd is required");
		}

		String token = getValidAccessToken();
		if (token == null || token.isBlank()) {
			throw new IllegalStateException("KIS access token is missing");
		}

		String uri = UriComponentsBuilder.fromPath(properties.getStockInfo().getPath())
				.queryParam("PRDT_TYPE_CD", safePrdtTypeCd)
				.queryParam("PDNO", pdno)
				.build(true)
				.toUriString();

		KisStockInfoResponse response;
		try {
			response = webClient.get()
					.uri(uri)
					.accept(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.header("appkey", properties.getAppkey())
					.header("appsecret", properties.getAppsecret())
					.header("tr_id", properties.getStockInfo().getTrId())
					.header("custtype", properties.getCusttype())
					.retrieve()
					.bodyToMono(KisStockInfoResponse.class)
					.block(Duration.ofSeconds(5));
		} catch (WebClientResponseException e) {
			logKisError("search-stock-info", uri, properties.getStockInfo().getTrId(), e);
			throw e;
		}

		if (response == null) {
			throw new IllegalStateException("Empty response from KIS stock info API");
		}
		if (!"0".equals(response.getRtCd())) {
			throw new IllegalStateException("KIS error: " + response.getMsgCd() + " - " + response.getMsg1());
		}
		return response;
	}

	/**
	 * 차트 데이터(일/주/월) 조회 (inquire-daily-itemchartprice)
	 * - start/end: YYYYMMDD
	 * - periodDivCode: D/W/M (기본값은 properties.kis.daily-chart.period-div-code)
	 * - 화면 그래프(캔들/라인) 데이터로 사용
	 */
	public KisDailyChartResponse fetchDailyChart(String stockId, String start, String end, String periodDivCode) {
		requireBasicConfig();
		requireDailyChartConfig();

		if (stockId == null || stockId.isBlank()) {
			throw new IllegalArgumentException("stockId is required");
		}

		String token = getValidAccessToken();
		if (token == null || token.isBlank()) {
			throw new IllegalStateException("KIS access token is missing");
		}

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
			response = webClient.get()
					.uri(uri)
					.accept(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.header("appkey", properties.getAppkey())
					.header("appsecret", properties.getAppsecret())
					.header("tr_id", properties.getDailyChart().getTrId())
					.header("custtype", properties.getCusttype())
					.retrieve()
					.bodyToMono(KisDailyChartResponse.class)
					.block(Duration.ofSeconds(5));
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

	/**
	 * 복수 종목 현재가 조회 (intstock-multprice)
	 * - KIS 문서 형식: FID_COND_MRKT_DIV_CODE_1..30, FID_INPUT_ISCD_1..30
	 * - 우리는 검색 결과 페이지에서 최대 20개까지만 요청하도록 상위 서비스에서 제한
	 * - 반환은 Map(원본 JSON) 그대로이고, StockService에서 프론트가 쓰기 좋게 정규화함
	 */
	public Map<?, ?> fetchMultiplePrices(List<String> stockIds) {
		requireBasicConfig();
		requireMultiPriceConfig();

		if (stockIds == null || stockIds.isEmpty()) {
			throw new IllegalArgumentException("stockIds are required");
		}

		String token = getValidAccessToken();
		if (token == null || token.isBlank()) {
			throw new IllegalStateException("KIS access token is missing");
		}

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
			return webClient.get()
					.uri(uri)
					.accept(MediaType.APPLICATION_JSON)
					.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
					.header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
					.header("appkey", properties.getAppkey())
					.header("appsecret", properties.getAppsecret())
					.header("tr_id", properties.getMultiPrice().getTrId())
					.header("custtype", properties.getCusttype())
					.retrieve()
					.bodyToMono(Map.class)
					.block(Duration.ofSeconds(5));
		} catch (WebClientResponseException e) {
			logKisError("intstock-multprice", uri, properties.getMultiPrice().getTrId(), e);
			throw e;
		}
	}

	private void logKisError(String apiName, String uri, String trId, WebClientResponseException e) {
		// KIS에서 내려주는 msg_cd/msg1를 확인해야 원인(레이트리밋/입력오류 등) 파악 가능
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

	private void requireBasicConfig() {
		// baseUrl/appkey/appsecret은 모든 KIS API의 공통 필수값
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

	private void requireVolumeRankConfig() {
		// 거래량 랭킹 API 호출에 필요한 설정값 검증
		if (properties.getVolumeRank() == null) {
			throw new IllegalStateException("kis.volume-rank is required");
		}
		if (properties.getVolumeRank().getPath() == null || properties.getVolumeRank().getPath().isBlank()) {
			throw new IllegalStateException("kis.volume-rank.path is required");
		}
		if (properties.getVolumeRank().getTrId() == null || properties.getVolumeRank().getTrId().isBlank()) {
			throw new IllegalStateException("kis.volume-rank.tr-id is required");
		}
	}

	private void requireStockInfoConfig() {
		// 종목 기본정보 API 호출에 필요한 설정값 검증
		if (properties.getStockInfo() == null) {
			throw new IllegalStateException("kis.stock-info is required");
		}
		if (properties.getStockInfo().getPath() == null || properties.getStockInfo().getPath().isBlank()) {
			throw new IllegalStateException("kis.stock-info.path is required");
		}
		if (properties.getStockInfo().getTrId() == null || properties.getStockInfo().getTrId().isBlank()) {
			throw new IllegalStateException("kis.stock-info.tr-id is required");
		}
	}

	private void requireDailyChartConfig() {
		// 차트 API 호출에 필요한 설정값 검증 (tr_id 누락 시 "kis.daily-chart.tr-id is required" 발생)
		if (properties.getDailyChart() == null) {
			throw new IllegalStateException("kis.daily-chart is required");
		}
		if (properties.getDailyChart().getPath() == null || properties.getDailyChart().getPath().isBlank()) {
			throw new IllegalStateException("kis.daily-chart.path is required");
		}
		if (properties.getDailyChart().getTrId() == null || properties.getDailyChart().getTrId().isBlank()) {
			throw new IllegalStateException("kis.daily-chart.tr-id is required");
		}
	}

	private void requireMultiPriceConfig() {
		// 복수 현재가 API 호출에 필요한 설정값 검증
		if (properties.getMultiPrice() == null) {
			throw new IllegalStateException("kis.multi-price is required");
		}
		if (properties.getMultiPrice().getPath() == null || properties.getMultiPrice().getPath().isBlank()) {
			throw new IllegalStateException("kis.multi-price.path is required");
		}
		if (properties.getMultiPrice().getTrId() == null || properties.getMultiPrice().getTrId().isBlank()) {
			throw new IllegalStateException("kis.multi-price.tr-id is required");
		}
	}

	private void requireRiseFallRankConfig() {
		if (properties.getRiseFallRank() == null) {
			throw new IllegalStateException("kis.rise-fall-rank is required");
		}
		if (properties.getRiseFallRank().getPath() == null || properties.getRiseFallRank().getPath().isBlank()) {
			throw new IllegalStateException("kis.rise-fall-rank.path is required");
		}
		if (properties.getRiseFallRank().getTrId() == null || properties.getRiseFallRank().getTrId().isBlank()) {
			throw new IllegalStateException("kis.rise-fall-rank.tr-id is required");
		}
	}

	private void requireMarketCapRankConfig() {
		if (properties.getMarketCapRank() == null) {
			throw new IllegalStateException("kis.market-cap-rank is required");
		}
		if (properties.getMarketCapRank().getPath() == null || properties.getMarketCapRank().getPath().isBlank()) {
			throw new IllegalStateException("kis.market-cap-rank.path is required");
		}
		if (properties.getMarketCapRank().getTrId() == null || properties.getMarketCapRank().getTrId().isBlank()) {
			throw new IllegalStateException("kis.market-cap-rank.tr-id is required");
		}
	}

	private Duration timeout() {
		Duration t = properties.getVolumeRank().getTimeout();
		return t == null ? Duration.ofSeconds(5) : t;
	}

	private static int parseIntSafe(String value) {
		try {
			return Integer.parseInt(value);
		} catch (Exception e) {
			return Integer.MAX_VALUE;
		}
	}

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

	private static long parseLongSafe(String value) {
		try {
			if (value == null) {
				return Long.MIN_VALUE;
			}
			String cleaned = value.replaceAll("[^0-9\\-]", "");
			if (cleaned.isBlank() || "-".equals(cleaned)) {
				return Long.MIN_VALUE;
			}
			return Long.parseLong(cleaned);
		} catch (Exception e) {
			return Long.MIN_VALUE;
		}
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
