package com.project.dykj.domain.member.mapper;

import java.util.Date;
import java.util.List;
import java.util.Map;

import org.apache.ibatis.annotations.Mapper;
import com.project.dykj.domain.member.entity.Member;

@Mapper
public interface MemberMapper {

	int checkId(String userId);

	int insertMember(Member member);

	Member findByUserId(String userId);

	List<String> selectAllMemberIds(); // 전체회원 조회해서 알림 보내기 주식장 닫,열

	int updateMemberStatus(String userId, String status);

	List<Member> selectAllMembers(Map<String, Object> params);

	int selectTotalMemberCount();

	int updateBanLimitDate(String userId, Date banLimitDate);
}
