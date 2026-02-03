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
public class AttendanceDTO {
	private boolean success;
	private String message;
	private int gainedExp;
	private int cumulativeDays;
	private boolean levelUp;
	private int currentLevel;
}
