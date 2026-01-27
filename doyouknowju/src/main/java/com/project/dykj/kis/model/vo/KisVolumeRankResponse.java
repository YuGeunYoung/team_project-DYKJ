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
public class KisVolumeRankResponse {

	@JsonIgnoreProperties(ignoreUnknown = true)
	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class OutputItem {

		@JsonProperty("hts_kor_isnm")
		private String htsKorIsnm;

		@JsonProperty("mksc_shrn_iscd")
		private String mkscShrnIscd;

		@JsonProperty("data_rank")
		private String dataRank;

		@JsonProperty("stck_prpr")
		private String stckPrpr;

		@JsonProperty("prdy_vrss_sign")
		private String prdyVrssSign;

		@JsonProperty("prdy_vrss")
		private String prdyVrss;

		@JsonProperty("prdy_ctrt")
		private String prdyCtrt;

		@JsonProperty("acml_vol")
		private String acmlVol;

		@JsonProperty("prdy_vol")
		private String prdyVol;

		@JsonProperty("lstn_stcn")
		private String lstnStcn;

		@JsonProperty("avrg_vol")
		private String avrgVol;

		@JsonProperty("n_befr_clpr_vrss_prpr_rate")
		private String nBefrClprVrssPrprRate;

		@JsonProperty("vol_inrt")
		private String volInrt;

		@JsonProperty("vol_tnrt")
		private String volTnrt;

		@JsonProperty("nday_vol_tnrt")
		private String ndayVolTnrt;

		@JsonProperty("avrg_tr_pbmn")
		private String avrgTrPbmn;

		@JsonProperty("tr_pbmn_tnrt")
		private String trPbmnTnrt;

		@JsonProperty("nday_tr_pbmn_tnrt")
		private String ndayTrPbmnTnrt;

		@JsonProperty("acml_tr_pbmn")
		private String acmlTrPbmn;
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
