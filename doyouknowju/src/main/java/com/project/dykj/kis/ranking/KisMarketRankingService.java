package com.project.dykj.kis.ranking;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.project.dykj.kis.service.KisService;
import com.project.dykj.kis.model.vo.RiseFallRankItem;
import com.project.dykj.kis.model.vo.VolumeRankItem;
import com.project.dykj.kis.model.vo.TradeAmountRankItem;

@Service
@ConditionalOnProperty(name = "ranking.source", havingValue = "kis")
public class KisMarketRankingService implements MarketRankingService {

	private final KisService kisService;

	public KisMarketRankingService(KisService kisService) {
		this.kisService = kisService;
	}

	@Override
	public List<VolumeRankItem> getVolumeTop10() {
		return kisService.getVolumeTop10();
	}

	@Override
	public List<TradeAmountRankItem> getTradeAmountTop10() {
		return kisService.getTradeAmountTop10();
	}

	@Override
	public List<RiseFallRankItem> getRiseRateTop10() {
		return kisService.getRiseRateTop10();
	}

	@Override
	public List<RiseFallRankItem> getFallRateTop10() {
		return kisService.getFallRateTop10();
	}
}
