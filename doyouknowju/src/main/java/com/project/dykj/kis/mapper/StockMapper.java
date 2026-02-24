package com.project.dykj.kis.mapper;

import java.util.List;

import org.apache.ibatis.annotations.Param;

import com.project.dykj.kis.model.vo.StockSearchItem;
import com.project.dykj.kis.model.vo.StockSuggestItem;

public interface StockMapper {

	List<StockSuggestItem> suggest(@Param("q") String q, @Param("limit") int limit);

	List<StockSearchItem> search(@Param("q") String q, @Param("offset") int offset, @Param("size") int size);
}
