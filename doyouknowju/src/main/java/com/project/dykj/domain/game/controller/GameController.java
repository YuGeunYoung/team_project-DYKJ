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
import com.project.dykj.domain.member.entity.Member;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/api/game")
@RequiredArgsConstructor
@CrossOrigin(origins = "http://localhost:5173", allowCredentials = "true")
public class GameController {
	private final GameService gameService;
	
	@PostMapping("/exp/test")
    public ResponseEntity<?> testGainExp(
            @RequestBody Map<String, Object> request, HttpSession session) {

        Member loginUser = (Member) session.getAttribute("loginUser");

        if (loginUser == null) {
            return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }

        String userId = loginUser.getUserId();
        int amount = (Integer) request.get("amount");

        ExpResultDTO result = gameService.gainExp(userId, amount, "TEST_ADMIN");

        return ResponseEntity.ok(result);
    }
	
}
