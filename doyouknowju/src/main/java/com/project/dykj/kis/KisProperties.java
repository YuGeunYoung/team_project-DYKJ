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

	@Data
	public static class VolumeRank {
		private String path;
		private String trId;
		private Duration timeout = Duration.ofSeconds(5);
	}

	@Data
	public static class Fid {
		private String condMrktDivCode = "J";
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
}
