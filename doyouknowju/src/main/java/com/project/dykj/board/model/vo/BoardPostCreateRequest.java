package com.project.dykj.board.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardPostCreateRequest {

	private String boardType;
	private String stockId;
	private String userId;
	private String title;
	private String content;
}
