package com.project.dykj.domain.trade_history.dto.res;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class TradeHistoryRes {
    private String stockId;
    private String stockName;
    private String tradeCategory;
    private int totalTradePrice;
    private int tradeCount;
    private int stockPrice;
    private long afterBalance;
    private String tradeDate;
}
