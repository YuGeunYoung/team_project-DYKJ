package com.project.dykj.domain.member.service;

import java.util.Optional;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.member.dto.LoginRequestDTO;
import com.project.dykj.domain.member.dto.MemberRequestDTO;
import com.project.dykj.domain.member.entity.Member;
import com.project.dykj.domain.member.repository.MemberRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {
	
	private final MemberRepository memberRepository;
	private final PasswordEncoder passwordEncoder;
	
	@Transactional
	public String signUp(MemberRequestDTO dto) {
		// 아이디 중복 여부 확인
		if(memberRepository.existsById(dto.getUserId())) {
			return "ALREADY_EXISTS"; 
		}
		
		String encodedPassword = passwordEncoder.encode(dto.getUserPwd());
		
		Member member = Member.builder()
						.userId(dto.getUserId())
						.userPwd(encodedPassword)
						.phone(dto.getPhone())
						.isReceiveNotification(dto.getIsReceiveNotification())
						.build();
		
		memberRepository.save(member);
		
		return "SUCCESS";
	}
	
//	public boolean login(LoginRequestDTO loginRequest) {
//		Optional<Member> memberOpt = memberRepository.findById(loginRequest.getUserId());
//		
//		if(memberOpt.isPresent()) {
//			Member member = memberOpt.get();
//			//비밀번호 일치 여부
//			return passwordEncoder.matches(loginRequest.getUserPwd(), member.getUserPwd());
//		}
//		return false; 
//	}
}
