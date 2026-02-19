package com.project.dykj.domain.member.service;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
		
		// 제재 기한 자동 해제
		if(member.getBanLimitDate() != null) {
			Date now = new Date();
			if(member.getBanLimitDate().before(now)) {
				memberMapper.updateBanLimitDate(member.getUserId(), null);
				member.setBanLimitDate(null);
			}
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
	
	// 관리자 전체 회원 목록
		public Map<String, Object> getAllMembers(int page, int size) {
			int offset = (page - 1) * size;
			Map<String, Object> params = new HashMap<>();
			params.put("offset", offset);
			params.put("size", size);

			List<Member> members = memberMapper.selectAllMembers(params);
			int total = memberMapper.selectTotalMemberCount();

			Map<String, Object> result = new HashMap<>();
			result.put("members", members);
			result.put("total", total);
			result.put("page", page);
			result.put("size", size);
			return result;
		}

		// 관리자 제재 처리
		@Transactional
		public boolean banMember(String userId, int banDays) {
			Date banLimitDate = null;
			if (banDays > 0) {
				Calendar cal = Calendar.getInstance();
				cal.add(Calendar.DAY_OF_YEAR, banDays);
				// 시간은 23:59:59로 설정하거나 단순 날짜로 처리
				banLimitDate = cal.getTime();
			} else if (banDays >= 9999) {
				// 영구 정지
				Calendar cal = Calendar.getInstance();
				cal.set(9999, Calendar.DECEMBER, 31);
				banLimitDate = cal.getTime();
			}
			// banDays == 0 이면 null이 전달되어 해제됨
			return memberMapper.updateBanLimitDate(userId, banLimitDate) > 0;
		}
}
