package com.project.dykj.kis.model.vo;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 등락률 Top 리스트 출력용 DTO
 * - stckShrnIscd: 종목코드
 * - htsKorIsnm: 종목명
 * - stckPrpr: 현재가
 * - prdyVrssSign/prdyVrss/prdyCtrt: 전일 대비 부호/전일 대비/전일 대비율(%)
 * - acmlVol: 누적 거래량(선택 표시)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class RiseFallRankItem {
	/**
	 * 기존 거래량 TOP10(VolumeRankItem)과 프론트 표시 필드를 맞추기 위해
	 * 종목코드는 mksc_shrn_iscd 키로 내려줍니다.
	 * - KIS 등락률 순위 응답의 stck_shrn_iscd 값을 그대로 매핑
	 */
	@JsonProperty("mksc_shrn_iscd")
	private String mkscShrnIscd;

	@JsonProperty("hts_kor_isnm")
	private String htsKorIsnm;

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
}
