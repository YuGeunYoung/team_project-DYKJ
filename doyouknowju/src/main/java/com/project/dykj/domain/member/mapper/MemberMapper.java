package com.project.dykj.domain.member.mapper;

import java.util.Optional;
import org.apache.ibatis.annotations.Mapper;
import com.project.dykj.domain.member.entity.Member;

@Mapper
public interface MemberMapper {
	
	int checkId(String userId);
	int insertMember(Member member);
	
	Member findByUserId(String userId);
}
