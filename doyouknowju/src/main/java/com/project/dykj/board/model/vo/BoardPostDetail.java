package com.project.dykj.board.model.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardPostDetail {

	private long postId;
	private String boardType;
	private String stockId;
	private String userId;
	private String title;
	private String content;
	private long viewCnt;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
}
