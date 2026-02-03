package com.project.dykj.domain.stock.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GetHoldingsByUserIdRes {
    private String userId; // 회원 ID
    private String stockId; // 종목 ID
    private long totalPrice; // 총 매수 금액
    private long totalCount; // 총 매수 수량
    private String stockName; // 종목명
    private long currentPrice; // 현재가
    private long profitAndLoss; // 평가 손익
    private double profitAndLossRate; // 평가 손익률
}
