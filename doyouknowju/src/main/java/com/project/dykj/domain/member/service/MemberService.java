package com.project.dykj.domain.member.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.member.dto.LoginRequestDTO;
import com.project.dykj.domain.member.entity.Member;
import com.project.dykj.domain.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MemberService {
	
	private final MemberMapper memberMapper;
	private final PasswordEncoder passwordEncoder;
	
	@Transactional
	public boolean signUp(Member member) {
		member.setUserPwd(passwordEncoder.encode(member.getUserPwd()));
		int result = memberMapper.insertMember(member);
		return result > 0;
	}
	
	public Member login(LoginRequestDTO loginRequest) {
		Member member = memberMapper.findByUserId(loginRequest.getUserId());
		if(member == null) {
			return null;
		}
		
		boolean pwdMatch = passwordEncoder.matches(loginRequest.getUserPwd(), member.getUserPwd());
		if(pwdMatch) {
			return member;
		}else {
			return null;
		}
	}
	
	public Member getMemberById(String userId) {
		return memberMapper.findByUserId(userId);
	}
	
	public boolean checkId(String userId) {
		return memberMapper.checkId(userId) > 0;
	}
	
	@Transactional
	public boolean withdraw(String userId, String password) {
		Member member = memberMapper.findByUserId(userId);
		if(member == null) return false;
		
		if(passwordEncoder.matches(password, member.getUserPwd())) {
			return memberMapper.updateMemberStatus(userId, "N") > 0;
		}
		return false;
	}
}
