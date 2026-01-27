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

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;

import com.project.dykj.kis.KisProperties;
import com.project.dykj.kis.model.vo.KisStockInfoResponse;
import com.project.dykj.kis.model.vo.KisVolumeRankResponse;
import com.project.dykj.kis.model.vo.VolumeRankItem;

@Service
public class KisService {

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
	 * KIS access_token 발급/갱신 (client_credentials)
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
	 * 토큰이 없으면 자동 발급 후 반환
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
	 * 거래량 순위(Top10) 조회 결과를 프론트용 VO로 변환
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
	 * KIS 거래량 순위 원본 응답 조회(필요 시 확장용)
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

		KisVolumeRankResponse response = webClient.get()
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

		if (response == null) {
			throw new IllegalStateException("Empty response from KIS volume rank API");
		}
		if (!"0".equals(response.getRtCd())) {
			throw new IllegalStateException("KIS error: " + response.getMsgCd() + " - " + response.getMsg1());
		}
		return response;
	}

	/**
	 * 실시간 현재가 조회(inquire-price) - 반환은 Map 형태(추후 VO로 교체 가능)
	 */
	public Map<?, ?> getStockPrice(String stockCode) {
		requireBasicConfig();

		String token = getValidAccessToken();
		if (token == null || token.isBlank()) {
			throw new IllegalStateException("KIS access token is missing");
		}

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
	}

	/**
	 * 종목 상세 정보 조회(search-stock-info)
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

		KisStockInfoResponse response = webClient.get()
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

		if (response == null) {
			throw new IllegalStateException("Empty response from KIS stock info API");
		}
		if (!"0".equals(response.getRtCd())) {
			throw new IllegalStateException("KIS error: " + response.getMsgCd() + " - " + response.getMsg1());
		}
		return response;
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
