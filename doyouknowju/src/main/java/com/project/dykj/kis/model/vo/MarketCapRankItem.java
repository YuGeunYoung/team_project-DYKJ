package com.project.dykj.kis.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * KIS 시가총액 순위 API 응답을 프론트에서 쓰기 좋게 정규화한 DTO
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MarketCapRankItem {
	private String stockId;
	private String stockName;
	private String currentPrice;
	private String changeSign;
	private String changeValue;
	private String changeRate;
	private String marketCap;
	private String dataRank;
}

