package com.project.dykj.domain.notification.repository;

import org.apache.ibatis.annotations.Mapper;
import java.util.List;
import com.project.dykj.domain.stock.vo.Holding;

@Mapper
public interface HoldingMapper {
    // 모든 유저의 보유 주식 현황을 가져옵니다.
    List<Holding> selectAllHoldings();

    // 특정 유저의 보유 주식 현황을 가져옵니다.
    List<Holding> selectHoldingByUserId(String userId);

    // 특정 유저의 특정 종목 보유 주식 현황 가져오기
    long getHoldingTotalCountByUserIdAndStockId(String userId, String stockId);
}