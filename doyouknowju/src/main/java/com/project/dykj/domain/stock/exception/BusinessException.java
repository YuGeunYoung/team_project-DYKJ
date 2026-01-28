package com.project.dykj.domain.stock.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;

// 비즈니스 로직 상에서 발생되는 예외들을 따로 관리하기 위한 Exception Class.
@Getter
@AllArgsConstructor
public class BusinessException extends RuntimeException {
    private final ErrorCode errorCode;
}
