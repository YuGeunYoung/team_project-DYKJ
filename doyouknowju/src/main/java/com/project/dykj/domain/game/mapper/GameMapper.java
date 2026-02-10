package com.project.dykj.domain.game.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.project.dykj.domain.game.dto.AchievementDTO;
import com.project.dykj.domain.game.dto.TitleDTO;
import com.project.dykj.domain.game.entity.ExpHistory;
import com.project.dykj.domain.game.entity.LevelPolicy;
import com.project.dykj.domain.game.entity.Quiz;

@Mapper
public interface GameMapper {
	List<LevelPolicy> selectLevelPolicies();
	
	int insertExpHistory(ExpHistory expHistory);
	
	int updateMemberExp(String userId, int amount);
	
	int updateMemberLevel(String userId, int level);

	int selectTodayAttendance(String userId);

	void insertAttendance(String userId);

	void updateCumulativeDays(String userId);

	List<String> selectAttendanceHistory(String userId);

	Quiz selectTodayQuiz();

	int checkTodaySolved(String userId);

	void insertMemberQuiz(String userId, int quizId, String isCorrect);

	List<AchievementDTO> selectUserAchievements(String userId);

	AchievementDTO getAchievementMasterInfo(int achievementId);

	int updateRewardStatus(String userId, int achievementId);

	void insertMemberAchievement(String userId, int achievementId);

	int selectAttendanceCount(String userId);

	int selectCorrectQuizCount(String userId);

	void insertMemberTitle(String userId, Integer rewardTitleId);

	List<TitleDTO> selectMemberTitles(String userId);

	int selectTotalQuizCount(String userId);

	int selectPostCount(String userId);

}
