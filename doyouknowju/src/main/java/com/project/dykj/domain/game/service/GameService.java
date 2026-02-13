package com.project.dykj.domain.game.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.project.dykj.domain.game.dto.AchievementDTO;
import com.project.dykj.domain.game.dto.AttendanceDTO;
import com.project.dykj.domain.game.dto.ExpResultDTO;
import com.project.dykj.domain.game.dto.QuizDTO;
import com.project.dykj.domain.game.dto.TitleDTO;
import com.project.dykj.domain.game.entity.ExpHistory;
import com.project.dykj.domain.game.entity.LevelPolicy;
import com.project.dykj.domain.game.entity.Quiz;
import com.project.dykj.domain.game.mapper.GameMapper;
import com.project.dykj.domain.member.entity.Member;
import com.project.dykj.domain.member.mapper.MemberMapper;
import com.project.dykj.domain.stock.mapper.TradeMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class GameService {
	private final GameMapper gameMapper;
	private final MemberMapper memberMapper;
	private final TradeMapper tradeMapper;
	
	private static final int ACHIEV_FIRST_ATTENDANCE = 1;
	private static final int ACHIEV_FIRST_QUIZ = 2;
	private static final int ACHIEV_FIRST_TRADE = 3;
	private static final int ACHIEV_FIRST_FAVORITE = 4;
	private static final int ACHIEV_PROFIT_1 = 5;
	// 누적 출석 도전과제 ID
	private static final int ACHIEV_ATTENDANCE_7 = 9;
	private static final int ACHIEV_ATTENDANCE_30 = 10;
	private static final int ACHIEV_ATTENDANCE_100 = 11;
	private static final int ACHIEV_ATTENDANCE_365 = 12;
	private static final int ACHIEV_ATTENDANCE_1000 = 13;
	// 레벨업 도전과제 ID
	private static final int ACHIEV_LEVEL_10 = 14;
	private static final int ACHIEV_LEVEL_30 = 15;
	private static final int ACHIEV_LEVEL_50 = 16;
	private static final int ACHIEV_LEVEL_100 = 17;
	// 누적 매매 도전과제 ID
    private static final int ACHIEV_TRADE_50 = 18;
    private static final int ACHIEV_TRADE_100 = 19;
    private static final int ACHIEV_TRADE_500 = 20;
    private static final int ACHIEV_TRADE_1000 = 21;
    // 관심 등록 도전과제 ID
    private static final int ACHIEV_FAVORITE_5 = 22;
    private static final int ACHIEV_FAVORITE_20 = 23;
    private static final int ACHIEV_FAVORITE_50 = 24;
    private static final int ACHIEV_FAVORITE_100 = 25;
    // 퀴즈 참여 도전과제 ID
    private static final int ACHIEV_QUIZ_JOIN_10 = 26;
    private static final int ACHIEV_QUIZ_JOIN_50 = 27;
    private static final int ACHIEV_QUIZ_JOIN_100 = 28;
    private static final int ACHIEV_QUIZ_JOIN_500 = 29;
    // 게시글 작성 도전과제 ID
    private static final int ACHIEV_BOARD_5 = 30;
    private static final int ACHIEV_BOARD_20 = 31;
    private static final int ACHIEV_BOARD_50 = 32;
    private static final int ACHIEV_BOARD_100 = 33;
    // 보유 포인트 도전과제 ID
    private static final int ACHIEV_WEALTH_20M = 34;
    private static final int ACHIEV_WEALTH_50M = 35;
    private static final int ACHIEV_WEALTH_100M = 36;
    private static final int ACHIEV_WEALTH_1B = 37;
    // 거래 금액 도전과제 ID
    private static final int ACHIEV_TX_10M = 38;
    private static final int ACHIEV_TX_50M = 39;
    private static final int ACHIEV_TX_100M = 40;
    private static final int ACHIEV_TX_1B = 41;
    
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
		for (LevelPolicy policy : policies) {
			// 사용자 레벨보다 높은 레벨 정책만 고려
			if (policy.getLevelId() > newLevel) {
				if (totalExp >= policy.getRequiredExp()) {
					newLevel = policy.getLevelId();
				} else {
					break;
				}
			}
		}
		
		boolean isLevelUp = false;
		if (newLevel > previousLevel) {
			gameMapper.updateMemberLevel(userId, newLevel);
			isLevelUp = true;
			
			// [도전과제] 레벨업 달성 처리 (>= 체크로 누락 방지)
			if (newLevel >= 100)
				recordAchievement(userId, ACHIEV_LEVEL_100);
			if (newLevel >= 50)
				recordAchievement(userId, ACHIEV_LEVEL_50);
			if (newLevel >= 30)
				recordAchievement(userId, ACHIEV_LEVEL_30);
			if (newLevel >= 10)
				recordAchievement(userId, ACHIEV_LEVEL_10);
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
		
		int attendanceCount = gameMapper.selectAttendanceCount(userId);
		// 누적 출석 도전과제 달성 처리 (>= 체크로 누락 방지)
		if (attendanceCount >= 1000)
			recordAchievement(userId, ACHIEV_ATTENDANCE_1000);
		if (attendanceCount >= 365)
			recordAchievement(userId, ACHIEV_ATTENDANCE_365);
		if (attendanceCount >= 100)
			recordAchievement(userId, ACHIEV_ATTENDANCE_100);
		if (attendanceCount >= 30)
			recordAchievement(userId, ACHIEV_ATTENDANCE_30);
		if (attendanceCount >= 7)
			recordAchievement(userId, ACHIEV_ATTENDANCE_7);
		if (attendanceCount >= 1)
			recordAchievement(userId, ACHIEV_FIRST_ATTENDANCE);
		
		// 보상 경험치
		int rewardExp = 50;
		
		ExpResultDTO expResult = gainExp(userId, rewardExp, "ATTENDANCE");
		Member member = memberMapper.findByUserId(userId);
		
		return AttendanceDTO.builder()
				.success(true)
				.message("출석 체크가 완료되었습니다! 50 EXP 획득!")
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
		if (gameMapper.checkTodaySolved(userId) > 0) {
			return QuizDTO.builder()
					.solved(true)
					.build();
		}
		
		Quiz quiz = gameMapper.selectTodayQuiz();
		if (quiz == null) return null;
		
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
		
		if (isCorrect) {
			int correctCount = gameMapper.selectCorrectQuizCount(userId);
			if (correctCount == 1) {
				recordAchievement(userId, ACHIEV_FIRST_QUIZ);
			}
		}
		
		QuizDTO result = QuizDTO.builder()
								.correct(isCorrect)
								.quizAnswer(quiz.getQuizAnswer())
								.quizExplain(quiz.getQuizExplain())
								.rewardExp(quiz.getQuizReward())
								.build();
		
		if (isCorrect) {
			ExpResultDTO expResult = gainExp(userId, quiz.getQuizReward(), "QUIZ");
			result.setLevelUp(expResult.isLevelUp());
			result.setCurrentLevel(expResult.getCurrentLevel());
		}
		
		// 퀴즈 참여 횟수 체크
		checkQuizParticipationAchievements(userId);
		
		return result;
	}

	public List<AchievementDTO> getAchievementList(String userId) {
		return gameMapper.selectUserAchievements(userId);
	}

	@Transactional
	public ExpResultDTO processRewardClaim(String userId, int achievementId) {
		AchievementDTO achiev = gameMapper.getAchievementMasterInfo(achievementId);
		
		int result = gameMapper.updateRewardStatus(userId, achievementId);
		
		if (result > 0 && achiev != null) {
			ExpResultDTO expResult = gainExp(userId, achiev.getRewardExp(), "ACHIEV_"+achievementId);
			
			if(achiev.getRewardTitleId() != null && achiev.getRewardTitleId() > 0) {
				gameMapper.insertMemberTitle(userId, achiev.getRewardTitleId());
			}
			return expResult;
		}
		return null;
	}
	
	@Transactional
	public void recordAchievement(String userId, int achievementId) {
		gameMapper.insertMemberAchievement(userId, achievementId);
	}

	public List<TitleDTO> getMyTitles(String userId) {
		return gameMapper.selectMemberTitles(userId);
	}

	@Transactional
	public void checkTradeAchievements(String userId) {
		int tradeCount = tradeMapper.selectTradeCount(userId);
		
		if(tradeCount >= 50)
			recordAchievement(userId, ACHIEV_TRADE_50);
		if(tradeCount >= 100)
			recordAchievement(userId, ACHIEV_TRADE_100);
		if(tradeCount >= 500)
			recordAchievement(userId, ACHIEV_TRADE_500);
		if(tradeCount >= 1000)
			recordAchievement(userId, ACHIEV_TRADE_1000);
	}
	
	@Transactional
	public void checkQuizParticipationAchievements(String userId) {
		int totalQuizCount = gameMapper.selectTotalQuizCount(userId);
		
		if(totalQuizCount >= 10)
			recordAchievement(userId, ACHIEV_QUIZ_JOIN_10);
		if(totalQuizCount >= 50)
			recordAchievement(userId, ACHIEV_QUIZ_JOIN_50);
		if(totalQuizCount >= 100)
			recordAchievement(userId, ACHIEV_QUIZ_JOIN_100);
		if(totalQuizCount >= 500)
			recordAchievement(userId, ACHIEV_QUIZ_JOIN_500);
	}
	
	@Transactional
	public void checkBoardAchievements(String userId) {
		int postCount = gameMapper.selectPostCount(userId);
		
		if (postCount >= 5)
			recordAchievement(userId, ACHIEV_BOARD_5);
		if (postCount >= 20)
			recordAchievement(userId, ACHIEV_BOARD_20);
		if (postCount >= 50)
			recordAchievement(userId, ACHIEV_BOARD_50);
		if (postCount >= 100)
			recordAchievement(userId, ACHIEV_BOARD_100);
	}
	
	@Transactional
	public void checkFavoriteStockAchievements(String userId) {
		int favoriteCount = gameMapper.selectFavoriteStockCount(userId);
		
		if(favoriteCount >= 1) {
			recordAchievement(userId, ACHIEV_FIRST_FAVORITE);
		}
		if(favoriteCount >= 5) {
			recordAchievement(userId, ACHIEV_FAVORITE_5);
		}
		if(favoriteCount >= 20) {
			recordAchievement(userId, ACHIEV_FAVORITE_20);
		}
		if(favoriteCount >= 50) {
			recordAchievement(userId, ACHIEV_FAVORITE_50);
		}
		if(favoriteCount >= 100) {
			recordAchievement(userId, ACHIEV_FAVORITE_100);
		}
	}

	public void equipTitle(String userId, int titleId) {
		gameMapper.resetEquippedTitles(userId);
		gameMapper.updateEquippedTitle(userId, titleId);
	}
	
	public void unequipTitle(String userId) {
		gameMapper.resetEquippedTitles(userId);
	}

	public List<TitleDTO> getEquippedTitlesForUsers(List<String> userIds) {
		if(userIds == null || userIds.isEmpty()) {
			return List.of();
		}
		return gameMapper.selectEquippedTitlesByUsers(userIds);
	}
	
	public void checkProfitAchievement(String userId, long totalPrice, long totalCount, long sellPrice) {
		if(totalCount == 0 || totalPrice == 0) {
			return;
		}
		long avgPrice = totalPrice / totalCount;
		
		if(avgPrice == 0) {
			return;
		}
		
		double profitRate = (double)(sellPrice - avgPrice) / avgPrice * 100;
		
		if(profitRate >= 1.0) {
			recordAchievement(userId, ACHIEV_PROFIT_1);
		}
	}
	
	public void checkWealthAchievements (String userId, long currentPoints) {
		if (currentPoints >= 20000000L) {
			recordAchievement(userId, ACHIEV_WEALTH_20M);
		}
		if (currentPoints >= 50000000L) {
			recordAchievement(userId, ACHIEV_WEALTH_50M);
		}
		if (currentPoints >= 100000000L) {
			recordAchievement(userId, ACHIEV_WEALTH_100M);
		}
		if (currentPoints >= 1000000000L) {
			recordAchievement(userId, ACHIEV_WEALTH_1B);
		}
	}
	
	public void checkTransactionAmountAchievements(String userId, long tradeAmount) {
		if (tradeAmount >= 10000000L) {
			recordAchievement(userId, ACHIEV_TX_10M);
		}
		if (tradeAmount >= 50000000L) {
			recordAchievement(userId, ACHIEV_TX_50M);
		}
		if (tradeAmount >= 100000000L) {
			recordAchievement(userId, ACHIEV_TX_100M);
		}
		if (tradeAmount >= 1000000000L) {
			recordAchievement(userId, ACHIEV_TX_1B);
		}
	}
	
	public void checkFirstTradeAchievement(String userId) {
		recordAchievement(userId, ACHIEV_FIRST_TRADE);
	}

}
