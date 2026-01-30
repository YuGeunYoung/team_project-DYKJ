package com.project.dykj.domain.stock.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/*
    주식 거래 성공 시 회원 정보에서 변하는 것은 잔액 뿐이다.
*/

@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
public class TradeRes {
    private long afterBalance;
}