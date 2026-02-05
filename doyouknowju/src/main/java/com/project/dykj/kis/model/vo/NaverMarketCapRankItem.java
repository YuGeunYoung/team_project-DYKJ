package com.project.dykj.kis.model.vo;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * 네이버 증권(stock.naver.com) 내부 API 기반 시가총액 TOP DTO
 * - marketSum: 시가총액
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class NaverMarketCapRankItem {
	private String stockId;
	private String stockName;
	private Long marketSum;
	private Long nowVal;
	private Long changeVal;
	private Double changeRate;
}

