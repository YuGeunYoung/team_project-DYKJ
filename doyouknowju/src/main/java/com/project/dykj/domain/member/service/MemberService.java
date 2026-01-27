package com.project.dykj.domain.member.service;

import com.project.dykj.domain.member.dto.MemberVO;

public interface MemberService {
	
	int checkId(String userId);
	
	void signup(MemberVO vo);

}
