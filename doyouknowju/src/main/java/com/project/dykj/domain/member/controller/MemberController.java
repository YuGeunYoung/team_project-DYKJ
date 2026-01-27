package com.project.dykj.domain.member.controller;

import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.project.dykj.domain.member.dto.MemberSignupDTO;
import com.project.dykj.domain.member.service.MemberService;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/members")
@RequiredArgsConstructor
public class MemberController {
	
	@Autowired
	private MemberService service;
	
	@PostMapping("/signup")
	public ResponseEntity<?> signup(@RequestBody MemberSignupDTO DTO){
		
		return null;
//		try {
//			service.signup(DTO);
//			return ResponseEntity.ok().body(Map.of("message","회원가입 성공"));
//		}catch(Exception e) {
//			return ResponseEntity.status(HttpStatusCode.INTERNAL_SERVER_ERROR)
//					 .body("파일 업로드 중 오류 발생");
//		}
	}
	

}
