package com.project.dykj.domain.game.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.game.dto.ExpResultDTO;
import com.project.dykj.domain.game.service.GameService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173")
public class GameController {
	private final GameService gameService;
	
	@PostMapping("/exp/test")
    public ResponseEntity<ExpResultDTO> testGainExp(
            @RequestBody Map<String, Object> request) {
        String userId = (String) request.get("userId");
        int amount = (Integer) request.get("amount");

        ExpResultDTO result = gameService.gainExp(userId, amount, "TEST_ADMIN");

        return ResponseEntity.ok(result);
    }
	
}
