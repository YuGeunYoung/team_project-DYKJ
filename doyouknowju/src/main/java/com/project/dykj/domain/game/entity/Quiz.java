package com.project.dykj.domain.game.entity;

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
public class Quiz {
	private int quizId;
	private String quizQuestion;
	private String quizAnswer;
	private String quizExplain;
	private int quizReward;
}
