package com.project.dykj.domain.member.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.member.dto.LoginRequestDTO;
import com.project.dykj.domain.member.entity.Member;
import com.project.dykj.domain.member.service.MemberService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
	
	private final MemberService memberService;
	
	@PostMapping("/signup")
	public ResponseEntity<?> signUp(@RequestBody Member member){
		boolean result = memberService.signUp(member);
		
		if(result) {
			return ResponseEntity.ok("SIGNUP_SUCCESS");
		}else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("SIGNUP_FAIL");
		}
	}
	
	//회원가입 시 아이디 중복 여부 확인 기능 추가
	@GetMapping("/checkId")
	public ResponseEntity<?> checkId(String userId){
		boolean isExists = memberService.checkId(userId);
		return ResponseEntity.ok(isExists);
	}
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest, HttpServletRequest request){
		Member loginMember = memberService.login(loginRequest);
		
		if(loginMember != null) {
			if(loginMember.getStatus().equals("N")) {
				Map<String, String> result = new HashMap<>();
				result.put("message", "탈퇴한 회원입니다.");
				return ResponseEntity.status(HttpStatus.FORBIDDEN).body(result);
			}
			HttpSession session = request.getSession();
			session.setAttribute("loginUser", loginMember);
	        
	        return ResponseEntity.ok(loginMember);
		}else {
			Map<String, String> error = new HashMap<>();
			error.put("message", "아이디 또는 비밀번호를 확인해주세요.");
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(error);
		}
	}
	
	//서버에 남아있는 세션 제거
	@PostMapping("/logout")
	public ResponseEntity<?> logout(HttpSession session){
		if(session != null) {
			session.invalidate();
		}
		return ResponseEntity.ok("Logout_SUCCESS");
	}
	
	//유저 정보 조회용 메소드
	@GetMapping("/info")
	public ResponseEntity<?> getMemberInfo(HttpSession session){
		Member loginUser = (Member)session.getAttribute("loginUser");
		
		//서버쪽 문제로 세션이 만료되었을 경우 인터셉터가 제대로 작동하지 않는 경우가 있음
		//따라서 정보 조회 수행 시에 로그인 정보 한번 더 확인
		if(loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
		}
		
		Member member = memberService.getMemberById(loginUser.getUserId());
		return ResponseEntity.ok(member);
	}
	
	@PostMapping("/withdraw")
	public ResponseEntity<?> withdraw(@RequestBody Map<String, String> request, HttpSession session){
		Member loginUser = (Member)session.getAttribute("loginUser");
		if(loginUser == null) {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("로그인이 필요합니다.");
		}
		
		String password = request.get("password");
		boolean result = memberService.withdraw(loginUser.getUserId(), password);
		
		if(result) {
			session.invalidate();
			return ResponseEntity.ok("WITHDRAW_SUCCESS");
		} else {
			return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("비밀번호가 일치하지 않거나 탈퇴 처리에 실패했습니다.");
		}
	}
}
