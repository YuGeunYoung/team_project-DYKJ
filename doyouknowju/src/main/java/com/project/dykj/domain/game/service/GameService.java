package com.project.dykj.domain.game.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.game.dto.AchievementDTO;
import com.project.dykj.domain.game.dto.AttendanceDTO;
import com.project.dykj.domain.game.dto.ExpResultDTO;
import com.project.dykj.domain.game.dto.QuizDTO;
import com.project.dykj.domain.game.entity.ExpHistory;
import com.project.dykj.domain.game.entity.LevelPolicy;
import com.project.dykj.domain.game.entity.Quiz;
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
		
		// 경험치 로그 기록
		ExpHistory history = ExpHistory.builder()
								.userId(userId)
								.gainedExp(amount)
								.sourceType(source)
								.build();
		gameMapper.insertExpHistory(history);
		
		// 회원 테이블 경험치 증가
		gameMapper.updateMemberExp(userId, amount);
		int totalExp = currentExp + amount;
		
		// 레벨업 체크
		List<LevelPolicy> policies = gameMapper.selectLevelPolicies();
		int newLevel = previousLevel;
		
		// 레벨 정책 확인
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
	
	public Member getMemberById(String userId) {
		return memberMapper.findByUserId(userId);
	}

	@Transactional
	public AttendanceDTO checkIn(String userId) {
		int todayCount = gameMapper.selectTodayAttendance(userId);
		if (todayCount > 0) {
			return AttendanceDTO.builder()
					.success(false)
					.message("오늘은 이미 출석체크를 완료하셨습니다.")
					.build();
		}
		
		gameMapper.insertAttendance(userId);
		gameMapper.updateCumulativeDays(userId);
		//보상 경험치
		int rewardExp = 100;
		
		ExpResultDTO expResult = gainExp(userId, rewardExp, "ATTENDANCE");
		Member member = memberMapper.findByUserId(userId);
		
		return AttendanceDTO.builder()
				.success(true)
				.message("출석 체크가 완료되었습니다! 100 EXP 획득!")
				.gainedExp(rewardExp)
				.cumulativeDays(member.getConsecDays())
				.levelUp(expResult.isLevelUp())
				.currentLevel(expResult.getCurrentLevel())
				.build();
	}

	public List<String> getAttendanceHistory(String userId) {
		return gameMapper.selectAttendanceHistory(userId);
	}

	public QuizDTO getTodayQuiz(String userId) {
		if(gameMapper.checkTodaySolved(userId) > 0) {
			return QuizDTO.builder()
					.solved(true)
					.build();
		}
		
		Quiz quiz = gameMapper.selectTodayQuiz();
		if(quiz == null) return null;
		
		return QuizDTO.builder()
				.quizId(quiz.getQuizId())
				.quizQuestion(quiz.getQuizQuestion())
				.solved(false)
				.rewardExp(quiz.getQuizReward())
				.build();
	}
	
	@Transactional
	public QuizDTO solveQuiz(String userId, int quizId, String answer) {
		Quiz quiz = gameMapper.selectTodayQuiz();
		
		boolean isCorrect = quiz.getQuizAnswer().equals(answer);
		
		gameMapper.insertMemberQuiz(userId, quizId, isCorrect ? "Y" : "N");
		
		QuizDTO result = QuizDTO.builder()
								.correct(isCorrect)
								.quizAnswer(quiz.getQuizAnswer())
								.quizExplain(quiz.getQuizExplain())
								.rewardExp(quiz.getQuizReward())
								.build();
		
		if(isCorrect) {
			ExpResultDTO expResult = gainExp(userId, quiz.getQuizReward(), "QUIZ");
			result.setLevelUp(expResult.isLevelUp());
			result.setCurrentLevel(expResult.getCurrentLevel());
		}
		return result;
	}

	public List<AchievementDTO> getAchievementList(String userId) {
		return gameMapper.selectUserAchievements(userId);
	}

	@Transactional
	public boolean processRewardClaim(String userId, int achievementId) {
		AchievementDTO achiev = gameMapper.getAchievementMasterInfo(achievementId);
		
		int result = gameMapper.updateRewardStatus(userId, achievementId);
		
		if(result > 0 && achiev != null) {
			gainExp(userId, achiev.getRewardExp(), "ACHIEV_"+achievementId);
			return true;
		}
		return false;
	}
	
	@Transactional
	public void recordAchievement(String userId, int achievementId) {
		gameMapper.insertMemberAchievement(userId, achievementId);
	}
}
