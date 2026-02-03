package com.project.dykj.domain.game.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;

import com.project.dykj.domain.game.entity.ExpHistory;
import com.project.dykj.domain.game.entity.LevelPolicy;

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
}
