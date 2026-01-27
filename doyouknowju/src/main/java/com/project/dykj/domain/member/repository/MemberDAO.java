package com.project.dykj.domain.member.repository;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.project.dykj.domain.member.dto.MemberVO;

@Repository
public class MemberDAO {

	public int checkId(SqlSessionTemplate sqlSession, String userId) {
		return sqlSession.selectOne("MemberMapper.checkId",userId);
	}

	public int insertMember(SqlSessionTemplate sqlSession, MemberVO vo) {
		return sqlSession.insert("MemberMapper.insertMember",vo);
	}

}
