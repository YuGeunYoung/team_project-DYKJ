package com.project.dykj.domain.member.controller;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.member.dto.LoginRequestDTO;
import com.project.dykj.domain.member.dto.MemberRequestDTO;
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
	
	@PostMapping("/login")
	public ResponseEntity<?> login(@RequestBody LoginRequestDTO loginRequest, HttpServletRequest request){
		Member loginMember = memberService.login(loginRequest);
		
		if(loginMember != null) {
			HttpSession session = request.getSession();
			session.setAttribute("loginUser", loginMember);
			
			Map<String, Object> responseData = new HashMap<>();
			responseData.put("userId", loginMember.getUserId());
	        responseData.put("phone", loginMember.getPhone());
	        responseData.put("points", loginMember.getPoints());
	        responseData.put("status", loginMember.getStatus());
	        responseData.put("userRole", loginMember.getUserRole());
	        responseData.put("enrollDate", loginMember.getEnrollDate());
	        responseData.put("consecDays", loginMember.getConsecDays());
	        responseData.put("isReceiveNotification", loginMember.getIsReceiveNotification());
	        responseData.put("experience", loginMember.getExperience());
	        responseData.put("userLevel", loginMember.getUserLevel());
	        
	        return ResponseEntity.ok(responseData);
		}else {
			return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("LOGIN_FAIL");
		}
	}
}
