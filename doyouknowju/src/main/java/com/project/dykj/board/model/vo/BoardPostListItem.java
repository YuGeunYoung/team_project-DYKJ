package com.project.dykj.board.model.vo;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardPostListItem {

	private long postId;
	private String boardType;
	private String stockId;
	private String userId;
	private String title;
	private long viewCnt;
	private LocalDateTime createdAt;
}
