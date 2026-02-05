package com.project.dykj.kis.model.vo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * KIS 등락률 순위 응답(필요 필드만 매핑)
 * - TR: FHPST01700000
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KisRiseFallRankResponse {

	@JsonIgnoreProperties(ignoreUnknown = true)
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OutputItem {
		@JsonProperty("stck_shrn_iscd")
		private String stckShrnIscd;

		@JsonProperty("data_rank")
		private String dataRank;

		@JsonProperty("hts_kor_isnm")
		private String htsKorIsnm;

		@JsonProperty("stck_prpr")
		private String stckPrpr;

		@JsonProperty("prdy_vrss")
		private String prdyVrss;

		@JsonProperty("prdy_vrss_sign")
		private String prdyVrssSign;

		@JsonProperty("prdy_ctrt")
		private String prdyCtrt;

		@JsonProperty("acml_vol")
		private String acmlVol;
	}

	@JsonProperty("rt_cd")
	private String rtCd;

	@JsonProperty("msg_cd")
	private String msgCd;

	@JsonProperty("msg1")
	private String msg1;

	@JsonProperty("output")
	@JsonAlias("Output")
	private List<OutputItem> output;
}

