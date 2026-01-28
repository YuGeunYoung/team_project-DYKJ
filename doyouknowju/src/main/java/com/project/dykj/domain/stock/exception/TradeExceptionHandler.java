package com.project.dykj.domain.stock.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class TradeExceptionHandler {
    
    // 비즈니스 로직 exception handler
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<String> handleBusinessException(BusinessException e) {
        return ResponseEntity
                .status(e.getErrorCode().getStatus())
                .body(e.getErrorCode().getMessage());
    }

    // 그 외 예상치 못한 모든 에러(NullPointer 등)는 500으로 처리
    @ExceptionHandler(Exception.class)
    public ResponseEntity<String> handleAllException(Exception e) {
        return ResponseEntity
                .status(500)
                .body("서버 내부 오류가 발생했습니다: " + e.getMessage());
    }
}
