package com.project.dykj.domain.game.controller;

import java.util.List;
import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.game.dto.AchievementDTO;
import com.project.dykj.domain.game.dto.AttendanceDTO;
import com.project.dykj.domain.game.dto.ExpResultDTO;
import com.project.dykj.domain.game.dto.QuizDTO;
import com.project.dykj.domain.game.dto.TitleDTO;
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
	
	@GetMapping("/achiev/list")
	public ResponseEntity<?> getAchievements(HttpSession session){
		Member loginUser = (Member) session.getAttribute("loginUser");
		if(loginUser == null) {
			return ResponseEntity.status(401).body("로그인이 필요합니다.");
		}	
			
		try {
			List<AchievementDTO> list = gameService.getAchievementList(loginUser.getUserId());
			return ResponseEntity.ok(list);
		}catch(Exception e) {
			log.error("도전과제 목록 조회 중 오류 발생: ", e);
			return ResponseEntity.status(500).body(Map.of("message", "정보를 불러오는 데 실패했습니다.", "success", false));
		}
	}
	
	@PostMapping("/achiev/claim")
	public ResponseEntity<?> claimReward(@RequestBody Map<String, Object> request, HttpSession session){
		Member loginUser = (Member)session.getAttribute("loginUser");
		if(loginUser == null) {
			return ResponseEntity.status(401).body("로그인이 필요합니다.");
		}
		
		try {
			int achievementId = ((Number)request.get("achievementId")).intValue();
			
			ExpResultDTO expResult = gameService.processRewardClaim(loginUser.getUserId(), achievementId);
			
			if(expResult != null) {
				Member updateMember = gameService.getMemberById(loginUser.getUserId());
				session.setAttribute("loginUser", updateMember);
				
				return ResponseEntity.ok(Map.of("message","보상이 수령되었습니다.","success", true, "expResult", expResult));
			}else {
				return ResponseEntity.badRequest().body(Map.of("message","이미 수령했거나 수령 조건이 맞지 않습니다.","success",false));
			}
		}catch(Exception e) {
			return ResponseEntity.status(500).body(Map.of("message", "보상 처리 중 서버 오류가 발생했습니다.", "success", false));
		}
	}
	
	@GetMapping("/titles")
	public ResponseEntity<?> getMyTitles(HttpSession session){
		Member loginUser = (Member) session.getAttribute("loginUser");
		if(loginUser == null) {
			return ResponseEntity.status(401).body("로그인이 필요합니다.");
		}
		
		try {
			List<TitleDTO> titles = gameService.getMyTitles(loginUser.getUserId());
			return ResponseEntity.ok(titles);
		}catch(Exception e) {
			return ResponseEntity.status(500).body(Map.of("message", "칭호 정보를 불러오는 데 실패했습니다.", "success", false));
		}
	}
	
	@PostMapping("/titles/equip")
	public ResponseEntity<?> equipTitle(@RequestBody Map<String, Object> request, HttpSession session){
		Member loginUser = (Member) session.getAttribute("loginUser");
		if(loginUser == null) {
			return ResponseEntity.status(401).body("로그인이 필요합니다.");
		}
		
		try {
			int titleId = ((Number) request.get("titleId")).intValue();
			gameService.equipTitle(loginUser.getUserId(), titleId);
			
			//세션 정보 갱신
			Member updateMember = gameService.getMemberById(loginUser.getUserId());
			session.setAttribute("loginUser", updateMember);
			
			return ResponseEntity.ok(Map.of("message", "칭호가 장착되었습니다.", "success", true));
		}catch(Exception e) {
			log.error("칭호 장착 중 오류 발생: ", e);
			return ResponseEntity.status(500).body(Map.of("message", "칭호 장착에 실패했습니다.", "success", false));
		}
	}
	
	@PostMapping("/titles/unequip")
	public ResponseEntity<?> unequipTitle(HttpSession session){
		Member loginUser = (Member) session.getAttribute("loginUser");
		if(loginUser == null) {
			return ResponseEntity.status(401).body("로그인이 필요합니다.");
		}
		
		try {
			gameService.unequipTitle(loginUser.getUserId());
			
			Member updateMember = gameService.getMemberById(loginUser.getUserId());
			session.setAttribute("loginUser", updateMember);
			
			return ResponseEntity.ok(Map.of("message", "칭호 장착이 해제되었습니다.", "success", true));
		}catch(Exception e) {
			log.error("칭호 장착 해제 중 오류 발생: ", e);
			return ResponseEntity.status(500).body(Map.of("message", "칭호 장착 해제에 실패했습니다.", "success", false));
		}
	}
	
	@PostMapping("/titles/equipped-list")
	public ResponseEntity<?> getEquippedTitleList(@RequestBody Map<String, List<String>> request){
		try {
			List<String> userIds = request.get("userIds");
			List<TitleDTO> list = gameService.getEquippedTitlesForUsers(userIds);
			return ResponseEntity.ok(list);
		}catch(Exception e) {
			log.error("착용한 칭호 목록 조회 중 오류 발생: ", e);
			return ResponseEntity.status(500).body(Map.of("message", "칭호 정보를 불러오는 데 실패했습니다.", "success", false));
		}
	}
}
