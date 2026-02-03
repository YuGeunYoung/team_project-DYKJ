package com.project.dykj.kis.ranking;

import java.util.List;

import com.project.dykj.kis.model.vo.TradeAmountRankItem;
import com.project.dykj.kis.model.vo.VolumeRankItem;

public interface MarketRankingService {

	List<VolumeRankItem> getVolumeTop10();

	List<TradeAmountRankItem> getTradeAmountTop10();
}
