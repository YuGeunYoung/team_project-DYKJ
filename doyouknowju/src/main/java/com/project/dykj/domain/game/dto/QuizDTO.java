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
public class QuizDTO {
	//퀴즈 정보
	private int quizId;
	private String quizQuestion;
	private boolean solved;
	
	//결과 정보
	private boolean correct;
	private String quizAnswer;
	private String quizExplain;
	private int rewardExp;
	private boolean levelUp;
	private int currentLevel;
}
