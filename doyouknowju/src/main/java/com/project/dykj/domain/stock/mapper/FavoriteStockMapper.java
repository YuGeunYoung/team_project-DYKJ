package com.project.dykj.domain.stock.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Mapper;
import org.springframework.stereotype.Repository;

import com.project.dykj.domain.stock.dto.req.UserIdAndStockIdReq;
import com.project.dykj.domain.stock.dto.res.GetFavoriteStocksRes;

@Mapper
@Repository
public interface FavoriteStockMapper {
    public int addFavorite(UserIdAndStockIdReq req);
    public int deleteFavorite(UserIdAndStockIdReq req);
    public List<GetFavoriteStocksRes> getFavoriteStocks(String userId);
    public int isFavorite(UserIdAndStockIdReq req);
}
