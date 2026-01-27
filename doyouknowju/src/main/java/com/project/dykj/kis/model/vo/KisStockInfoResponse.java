package com.project.dykj.kis.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@JsonIgnoreProperties(ignoreUnknown = true)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class KisStockInfoResponse {

	@JsonIgnoreProperties(ignoreUnknown = true)
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Output {

		@JsonProperty("pdno")
		private String pdno;

		@JsonProperty("prdt_name")
		private String prdtName;

		@JsonProperty("std_idst_clsf_cd_name")
		private String stdIdstClsfCdName;

		@JsonProperty("tr_stop_yn")
		private String trStopYn;

		@JsonProperty("lstg_abol_dt")
		private String lstgAbolDt;
	}

	@JsonProperty("rt_cd")
	private String rtCd;

	@JsonProperty("msg_cd")
	private String msgCd;

	@JsonProperty("msg1")
	private String msg1;

	@JsonProperty("output")
	private Output output;
}
