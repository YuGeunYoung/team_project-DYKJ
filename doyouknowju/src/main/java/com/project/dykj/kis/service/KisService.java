package com.project.dykj.kis.service;

import java.time.Duration;
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
import com.project.dykj.kis.model.vo.KisStockInfoResponse;
import com.project.dykj.kis.model.vo.KisVolumeRankResponse;
import com.project.dykj.kis.model.vo.VolumeRankItem;

@Service
public class KisService {

	private static final Logger log = LoggerFactory.getLogger(KisService.class);

	private final KisProperties properties;
	private final WebClient webClient;

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
	 * KIS access_token issue/refresh (client_credentials)
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
	 * Issue token automatically if missing
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
	 * Map volume rank (Top10) response to VO
	 */
	public List<VolumeRankItem> getVolumeTop10() {
		KisVolumeRankResponse response = fetchVolumeRank();
		List<KisVolumeRankResponse.OutputItem> output = response.getOutput();
		if (output == null) {
			return List.of();
		}

		return output.stream()
				.sorted(Comparator.comparingInt(it -> parseIntSafe(it.getDataRank())))
				.limit(10)
				.map(it -> new VolumeRankItem(
						it.getMkscShrnIscd(),
						it.getHtsKorIsnm(),
						it.getStckPrpr(),
						it.getPrdyCtrt(),
						it.getAcmlVol()))
				.toList();
	}

	/**
	 * Fetch raw volume rank response (extend as needed)
	 */
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
	 * Get current price (inquire-price) - returns Map (can be modeled as VO later)
	 */
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
	 * Fetch stock detail info (search-stock-info)
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
	 * Daily/weekly/monthly chart data (for graph)
	 * - start/end: YYYYMMDD
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
	 * Multiple stock prices (KIS intstock-multprice)
	 * - joins codes as: 005930|000660|035720 (KIS docs: up to 50)
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
}

