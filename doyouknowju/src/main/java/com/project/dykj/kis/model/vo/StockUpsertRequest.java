package com.project.dykj.kis.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockUpsertRequest {

	private String stockId;
	private String stockName;
	private String stockSector;
	private String stockInfo;
	private String isActive;
}
