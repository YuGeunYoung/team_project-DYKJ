package com.project.dykj.kis.ranking;

import java.util.List;

import com.project.dykj.kis.model.vo.VolumeRankItem;
import com.project.dykj.kis.model.vo.RiseFallRankItem;
import com.project.dykj.kis.model.vo.MarketCapRankItem;

public interface MarketRankingService {

	List<VolumeRankItem> getVolumeTop10();

	List<RiseFallRankItem> getRiseRateTop10();

	List<RiseFallRankItem> getFallRateTop10();

	List<MarketCapRankItem> getMarketCapTop10();
}
