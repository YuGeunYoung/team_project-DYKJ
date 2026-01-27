package com.project.dykj.kis.ranking;

import java.util.List;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import com.project.dykj.kis.service.KisService;
import com.project.dykj.kis.model.vo.VolumeRankItem;

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
}
