package com.project.dykj.kis;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "kis")
public class KisProperties {

	private String baseUrl;
	private String appkey;
	private String appsecret;
	private String accessToken;
	private String custtype = "P";

	private VolumeRank volumeRank = new VolumeRank();
	private Fid fid = new Fid();
	private StockInfo stockInfo = new StockInfo();
	private DailyChart dailyChart = new DailyChart();
	private MultiPrice multiPrice = new MultiPrice();
	private RiseFallRank riseFallRank = new RiseFallRank();
	private MarketCapRank marketCapRank = new MarketCapRank();
	private Top10WebSocket top10WebSocket = new Top10WebSocket();

	@Data
	public static class VolumeRank {
		private String path;
		private String trId;
		private Duration timeout = Duration.ofSeconds(5);
	}

	@Data
	public static class Fid {
		private String condMrktDivCode = "UN";
		private String condScrDivCode = "20171";
		private String inputIscd = "";
		private String divClsCode = "0";
		private String blngClsCode = "0";
		private String trgtClsCode = "0";
		private String trgtExlsClsCode = "0";
		private String inputPrice1 = "0";
		private String inputPrice2 = "0";
		private String volCnt = "10";
		private String inputDate1 = "";
	}

	@Data
	public static class StockInfo {
		private String path;
		private String trId;
		private String prdtTypeCd;
	}

	@Data
	public static class DailyChart {
		private String path;
		private String trId;
		/** D: ?? W: 二? M: ??*/
		private String periodDivCode = "D";
	/** 0: ?섏젙二쇨? 諛섏쁺, 1: ?섏젙二쇨? 誘몃컲??*/
	private String orgAdjPrc = "0";
	}

	@Data
	public static class MultiPrice {
		private String path;
		private String trId;
	}

	@Data
	public static class RiseFallRank {
		private String path;
		private String trId;
		/** 議곌굔 ?붾㈃ 遺꾨쪟 肄붾뱶 (湲곕낯 20170) */
		private String condScrDivCode = "20170";
		/** 議곌굔 ?쒖옣 遺꾨쪟 肄붾뱶 (J: KRX, NX: NXT) */
		private String condMrktDivCode = "J";
		/** ?낅젰 醫낅ぉ肄붾뱶 (0000: ?꾩껜, 0001: 肄붿뒪?? 1001: 肄붿뒪?? */
		private String inputIscd = "0000";
	}

	@Data
	public static class MarketCapRank {
		private String path;
		private String trId;
		/** 議곌굔 ?붾㈃ 遺꾨쪟 肄붾뱶 (湲곕낯 20174) */
		private String condScrDivCode = "20174";
		/** 議곌굔 ?쒖옣 遺꾨쪟 肄붾뱶 (J: KRX, NX: NXT) */
		private String condMrktDivCode = "J";
		/** ?낅젰 醫낅ぉ肄붾뱶 (0000: ?꾩껜, 0001: 肄붿뒪?? 1001: 肄붿뒪?? */
		private String inputIscd = "0000";
	}

	@Data
	public static class Top10WebSocket {
		/** WebSocket Top10 ?ъ슜 ?щ? */
		private boolean enabled = false;
		/** Approval key 諛쒓툒 寃쎈줈 */
		private String approvalPath = "/oauth2/Approval";
		/** KIS WebSocket URL (wss://...) */
		private String wsUrl;
		/** ?묒냽 ???꾩넚??援щ룆 ?꾨Ц 硫붿떆吏(JSON 臾몄옄?? */
		private String subscribeMessage;
		/** ?ъ뿰寃?吏??珥? */
		private long reconnectDelaySeconds = 3;
		/** stale 湲곗? ?쒓컙(珥?: ?대떦 ?쒓컙 ?숈븞 誘몄닔????stale濡?媛꾩＜ */
		private long staleAfterSeconds = 20;
	}
}

