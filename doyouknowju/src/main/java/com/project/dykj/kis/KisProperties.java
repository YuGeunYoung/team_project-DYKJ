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
		/**
		 * D: 일, W: 주, M: 월
		 */
		private String periodDivCode = "D";
		/**
		 * 0: 수정주가 반영, 1: 수정주가 미반영 (KIS 문서 기준)
		 */
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
		/**
		 * 조건 화면 분류 코드 (KIS 문서 예: 20170)
		 */
		private String condScrDivCode = "20170";
		/**
		 * 조건 시장 분류 코드 (J:KRX, NX:NXT). 기본은 KRX(J)
		 */
		private String condMrktDivCode = "J";
		/**
		 * 입력 종목코드(시장). 0000: 전체, 0001: 코스피, 1001: 코스닥 등(KIS 문서 기준)
		 */
		private String inputIscd = "0000";
	}

	@Data
	public static class MarketCapRank {
		private String path;
		private String trId;
		/**
		 * 조건 화면 분류 코드 (KIS 문서의 Unique key 값)
		 * - 정확한 값은 문서 기준으로 세팅 필요
		 */
		private String condScrDivCode = "20174";
		/**
		 * 조건 시장 분류 코드 (J:KRX, NX:NXT). 기본은 KRX(J)
		 */
		private String condMrktDivCode = "J";
		/**
		 * 입력 종목코드(시장). 0000: 전체, 0001: 코스피, 1001: 코스닥 등(KIS 문서 기준)
		 */
		private String inputIscd = "0000";
	}
}
