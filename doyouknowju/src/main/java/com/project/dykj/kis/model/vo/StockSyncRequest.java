package com.project.dykj.kis.model.vo;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class StockSyncRequest {

	private String prdtTypeCd;
	private List<String> stockIds;
}
