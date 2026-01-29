package com.project.dykj.domain.game.entity;

import java.sql.Date;

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
public class ExpHistory {
	private long expLogId;
	private String userId;
	private int gainedExp;
	private String sourceType;
	private long sourceId;
	private Date gainedAt;
}
