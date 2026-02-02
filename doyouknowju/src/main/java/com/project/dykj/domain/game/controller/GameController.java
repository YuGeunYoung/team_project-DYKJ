package com.project.dykj.domain.game.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
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
public class GameController {
	private final GameService gameService;
	
	@PostMapping("/exp/test")
    public ResponseEntity<?> testGainExp(
            @RequestBody Map<String, Object> request, HttpSession session) {

        Member loginUser = (Member) session.getAttribute("loginUser");
        
        if(loginUser == null) {
        	return ResponseEntity.status(401).body("로그인이 필요합니다.");
        }
        
        String userId = loginUser.getUserId();
        int amount = ((Number) request.get("amount")).intValue();

        ExpResultDTO result = gameService.gainExp(userId, amount, "TEST_ADMIN");
        
        //경험치,레벨 업데이트 후 정보 다시 반영
        Member updateMember = gameService.getMemberById(userId);
        session.setAttribute("loginUser",updateMember);
        
        return ResponseEntity.ok(result);
    }
	
}
