package com.project.dykj.domain.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TradesByStockIdRes {
    private String tradeCategory;
    private long totalTradePrice;
    private long tradeCount;
    private long stockPrice;
    private String tradeDate;
}
