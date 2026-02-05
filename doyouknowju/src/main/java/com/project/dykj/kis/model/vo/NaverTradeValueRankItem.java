package com.project.dykj.kis.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 네이버 증권(stock.naver.com) 내부 API 기반 거래대금(누적 거래대금) 랭킹 DTO
 * - accAmount: 누적 거래대금(원)
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NaverTradeValueRankItem {
	private String stockId;
	private String stockName;
	private Long accAmount;
	private Long nowVal;
	private Long changeVal;
	private Double changeRate;
}