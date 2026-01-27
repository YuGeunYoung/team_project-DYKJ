package com.project.dykj.board.controller;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class BoardExceptionHandler {

	@ExceptionHandler(IllegalArgumentException.class)
	@ResponseStatus(HttpStatus.BAD_REQUEST)
	public Map<String, Object> badRequest(IllegalArgumentException e) {
		return Map.of(
				"error", "BAD_REQUEST",
				"message", e.getMessage() == null ? "invalid request" : e.getMessage()
		);
	}
}