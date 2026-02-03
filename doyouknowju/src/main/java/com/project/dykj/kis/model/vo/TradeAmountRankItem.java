package com.project.dykj.kis.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TradeAmountRankItem {

	@JsonProperty("mksc_shrn_iscd")
	private String mkscShrnIscd;

	@JsonProperty("hts_kor_isnm")
	private String htsKorIsnm;

	@JsonProperty("stck_prpr")
	private String stckPrpr;

	@JsonProperty("prdy_ctrt")
	private String prdyCtrt;

	@JsonProperty("acml_vol")
	private String acmlVol;

	@JsonProperty("acml_tr_pbmn")
	private String acmlTrPbmn;
}

