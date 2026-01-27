package com.project.dykj.domain.stock.service;

import java.util.List;

import org.mybatis.spring.SqlSessionTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.project.dykj.domain.stock.dto.MyTradesRes;
import com.project.dykj.domain.stock.repository.TradeDao;

@Service
public class TradeService {

    @Autowired
    private TradeDao tradeDao;

    @Autowired
    private SqlSessionTemplate sqlSession;

    public List<MyTradesRes> selectMyTrades(String userId) {
        return tradeDao.selectMyTrades(sqlSession, userId);
    }
}
