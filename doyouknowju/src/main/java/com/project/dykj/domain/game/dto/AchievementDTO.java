package com.project.dykj.domain.game.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AchievementDTO {
	private int achievementId;
	private String achievName;
	private String achievDesc;
	private int rewardExp;
	private Integer rewardTitleId;
	
	private String achievedAt;
	private String isRewarded;
}
