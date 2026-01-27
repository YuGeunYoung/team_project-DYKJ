package com.project.dykj.board.model.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardCommentItem {

	private long commentId;
	private long postId;
	private String userId;
	private String content;
	private LocalDateTime createdAt;
}
