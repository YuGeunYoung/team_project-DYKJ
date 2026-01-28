package com.project.dykj.domain.stock.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.project.dykj.domain.stock.dto.res.StockInfoRes;

@Mapper
@Repository
public interface StockInfoMapper {
    
    public int insertStockInfo(StockInfoRes stockInfo);
}
