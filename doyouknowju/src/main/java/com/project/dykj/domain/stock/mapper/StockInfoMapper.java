package com.project.dykj.domain.stock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.springframework.stereotype.Repository;

import com.project.dykj.domain.stock.dto.res.StockInfoRes;

@Mapper
@Repository
public interface StockInfoMapper {

    public int updateStockInfoIsActiveToN();
    
    public int upsertStockInfo(@Param("list") List<StockInfoRes> stockInfoList);
}
