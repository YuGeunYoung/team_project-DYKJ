package com.project.dykj.domain.game.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.game.dto.ExpResultDTO;
import com.project.dykj.domain.game.entity.ExpHistory;
import com.project.dykj.domain.game.entity.LevelPolicy;
import com.project.dykj.domain.game.mapper.GameMapper;
import com.project.dykj.domain.member.entity.Member;
import com.project.dykj.domain.member.mapper.MemberMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {
	private final GameMapper gameMapper;
	private final MemberMapper memberMapper;
	
	@Transactional
	public ExpResultDTO gainExp(String userId, int amount, String source) {
		
		Member member = memberMapper.findByUserId(userId);
		int previousLevel = member.getUserLevel();
		int currentExp = member.getExperience();
		
		//경험치 로그 기록
		ExpHistory history = ExpHistory.builder()
								.userId(userId)
								.gainedExp(amount)
								.sourceType(source)
								.build();
		gameMapper.insertExpHistory(history);
		
		//회원 테이블 경험치 증가
		gameMapper.updateMemberExp(userId, amount);
		int totalExp = currentExp + amount;
		
		//레벨업 체크
		List<LevelPolicy> policies = gameMapper.selectLevelPolicies();
		int newLevel = previousLevel;
		
		//레벨 정책 확인
		for(LevelPolicy policy : policies) {
			//사용자 레벨보다 높은 레벨 정책만 고려
			if(policy.getLevelId() > newLevel) {
				if(totalExp >= policy.getRequiredExp()) {
					newLevel = policy.getLevelId();
				}else {
					break;
				}
			}
		}
		
		boolean isLevelUp = false;
		if(newLevel > previousLevel) {
			gameMapper.updateMemberLevel(userId, newLevel);
			isLevelUp = true;
		}
		
		return ExpResultDTO.builder()
					.previousLevel(previousLevel)
					.currentLevel(newLevel)
					.gainedExp(amount)
					.totalExp(totalExp)
					.isLevelUp(isLevelUp)
					.build();
	}
}
