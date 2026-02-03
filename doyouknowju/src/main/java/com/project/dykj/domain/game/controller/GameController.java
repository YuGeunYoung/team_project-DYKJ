package com.project.dykj.domain.game.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.game.dto.AttendanceDTO;
import com.project.dykj.domain.game.dto.ExpResultDTO;
import com.project.dykj.domain.game.dto.QuizDTO;
import com.project.dykj.domain.game.service.GameService;
import com.project.dykj.domain.member.entity.Member;

import jakarta.servlet.http.HttpSession;
import jakarta.websocket.Session;
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
	
	@PostMapping("/attend")
	public ResponseEntity<?> checkAttend(HttpSession session){
		Member loginUser = (Member) session.getAttribute("loginUser");
		if(loginUser == null) {
			return ResponseEntity.status(401).body("로그인이 필요합니다.");
		}
		
		AttendanceDTO result = gameService.checkIn(loginUser.getUserId());
		if(result.isSuccess()) {
			Member updatedMember = gameService.getMemberById(loginUser.getUserId());
			session.setAttribute("loginUser", updatedMember);
		}
		return ResponseEntity.ok(result);
	}
	
	@GetMapping("/attend/history")
	public ResponseEntity<?> checkhistory(HttpSession session){
		Member loginUser = (Member) session.getAttribute("loginUser");
		if(loginUser == null) {
			return ResponseEntity.status(401).body("로그인이 필요합니다.");
		}
		
		List<String> history = gameService.getAttendanceHistory(loginUser.getUserId());
		return ResponseEntity.ok(history);
	}
	
	@GetMapping("/quiz/today")
	public ResponseEntity<?> getTodayQuiz(HttpSession session){
		Member loginUser = (Member) session.getAttribute("loginUser");
		if(loginUser == null) {
			return ResponseEntity.status(401).body("로그인이 필요합니다.");
		}
		QuizDTO quiz = gameService.getTodayQuiz(loginUser.getUserId());
		if(quiz == null) {
			return ResponseEntity.notFound().build();
		}
		return ResponseEntity.ok(quiz);
	}
	
	@PostMapping("/quiz/solve")
	public ResponseEntity<?> solveQuiz(@RequestBody Map<String, Object> request, HttpSession session){
		Member loginUser = (Member) session.getAttribute("loginUser");
		if(loginUser == null) {
			return ResponseEntity.status(401).body("로그인이 필요합니다.");
		}
		
		int quizId = ((Number) request.get("quizId")).intValue();
		String answer = (String) request.get("answer");
		
		try {
			QuizDTO result = gameService.solveQuiz(loginUser.getUserId(), quizId, answer);
			if(result.isCorrect()) {
				Member updateMember = gameService.getMemberById(loginUser.getUserId());
				session.setAttribute("loginUser", updateMember);
			}
			return ResponseEntity.ok(result);
		}catch(Exception e) {
			return ResponseEntity.status(500).body(e.getMessage());
		}
	}
}
