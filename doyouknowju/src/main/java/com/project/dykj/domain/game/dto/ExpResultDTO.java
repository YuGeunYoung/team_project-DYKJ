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
public class ExpResultDTO {
	private int previousLevel;
	private int currentLevel;
	private int gainedExp;
	private int totalExp;
	private boolean isLevelUp;
}
