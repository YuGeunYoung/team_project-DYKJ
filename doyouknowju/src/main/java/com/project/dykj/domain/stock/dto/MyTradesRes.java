package com.project.dykj.domain.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class MyTradesRes {
    private String stockId;
    private String stockName;
    private String tradeCategory;
    private long totalTradePrice;
    private long tradeCount;
    private long stockPrice;
    private long afterBalance;
    private String tradeDate;
}
