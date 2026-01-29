package com.project.dykj.domain.stock.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public enum ErrorCode {

    NOT_ENOUGH_BALANCE(400, "잔액이 부족합니다."),
    NOT_ENOUGH_HOLDING(400, "보유 주식이 부족합니다."),
    NOT_FOUND_STOCK(404, "해당 종목을 찾을 수 없습니다."),
    NOT_FOUND_USER(404, "해당 사용자를 찾을 수 없습니다."),

    FAIL_TO_TRADE(500, "주식 거래에 실패했습니다."),
    
    INTERNAL_SERVER_ERROR(500, "서버 오류가 발생했습니다.");

    private final int status;
    private final String message;
}
