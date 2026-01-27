package com.project.dykj.domain.stock.repository;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.stereotype.Repository;

import com.project.dykj.domain.stock.dto.MyTradesRes;

@Repository
public class TradeDao {


    public List<MyTradesRes> selectMyTrades(SqlSessionTemplate sqlSession, String userId) {
        return sqlSession.selectList("TradeMapper.selectMyTrades", userId);
    }
}
