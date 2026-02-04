package com.project.dykj.domain.stock.dto.res;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 네이버 stocklist 내부 API의 원본 아이템(필요 필드만 매핑)
 * - JSON 키가 소문자(예: itemcode)라서 @JsonProperty로 매핑합니다.
 */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class NaverStockListItem {
	@JsonProperty("itemcode")
	private String itemCode;

	@JsonProperty("itemname")
	private String itemName;

	/** 누적 거래대금(원) - 문자열(숫자) */
	@JsonProperty("accAmount")
	private String accAmount;

	/** 현재가 */
	@JsonProperty("nowVal")
	private String nowVal;

	/** 전일 대비 */
	@JsonProperty("changeVal")
	private String changeVal;

	/** 전일 대비율(%) */
	@JsonProperty("changeRate")
	private String changeRate;
}

