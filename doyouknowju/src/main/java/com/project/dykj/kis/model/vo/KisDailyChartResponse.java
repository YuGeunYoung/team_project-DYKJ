package com.project.dykj.kis.model.vo;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KisDailyChartResponse {

	@JsonProperty("rt_cd")
	private String rtCd;

	@JsonProperty("msg_cd")
	private String msgCd;

	@JsonProperty("msg1")
	private String msg1;

	@JsonProperty("output1")
	private Output1 output1;

	@JsonProperty("output2")
	@JsonAlias("Output2")
	private List<Output2> output2;

	@JsonIgnoreProperties(ignoreUnknown = true)
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Output1 {
		@JsonProperty("hts_kor_isnm")
		private String htsKorIsnm;
	}

	@JsonIgnoreProperties(ignoreUnknown = true)
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Output2 {
		@JsonProperty("stck_bsop_date")
		private String stckBsopDate;

		@JsonProperty("stck_oprc")
		private String stckOprc;

		@JsonProperty("stck_hgpr")
		private String stckHgpr;

		@JsonProperty("stck_lwpr")
		private String stckLwpr;

		@JsonProperty("stck_clpr")
		private String stckClpr;

		@JsonProperty("acml_vol")
		private String acmlVol;
	}
}

