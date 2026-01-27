package com.project.dykj.board.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BoardPostUpdateRequest {

	private String stockId;
	private String title;
	private String content;
}
