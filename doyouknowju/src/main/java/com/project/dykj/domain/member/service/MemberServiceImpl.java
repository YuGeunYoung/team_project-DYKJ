package com.project.dykj.domain.member.service;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.dykj.domain.member.dto.MemberVO;
import com.project.dykj.domain.member.repository.MemberDAO;

@Service
public class MemberServiceImpl implements MemberService {
	
	@Autowired
    private MemberDAO memberDAO;
    @Autowired
    private SqlSessionTemplate sqlSession;

    @Override
    public int checkId(String userId) {
        return memberDAO.checkId(sqlSession,userId);
    }

    @Override
    public void signup(MemberVO vo) {
        memberDAO.insertMember(sqlSession,vo);
    }
}
