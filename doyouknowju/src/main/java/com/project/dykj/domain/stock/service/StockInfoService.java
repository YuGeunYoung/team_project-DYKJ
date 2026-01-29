package com.project.dykj.domain.stock.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.project.dykj.domain.stock.dto.res.StockInfoRes;
import com.project.dykj.domain.stock.mapper.StockInfoMapper;

import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockInfoService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private final StockInfoMapper stockInfoMapper;
    
    private List<StockInfoRes> parseJson(String jsonResult) {
        try {
            // JSON 파싱 및 루트노드 저장
            JsonNode root = objectMapper.readTree(jsonResult);

            // JSON 데이터 추출
            JsonNode itemNode = root.path("response")
                                    .path("body")
                                    .path("items")
                                    .path("item");
            
            // JSON 데이터가 없을 경우 빈 리스트 반환
            if (itemNode.isMissingNode() || itemNode.isEmpty()) {
                return Collections.emptyList();
            }

            // JSON 데이터가 있을 경우 StockInfoRes로 변환하여 반환
            return objectMapper.convertValue(itemNode, new TypeReference<List<StockInfoRes>>() {});
        } catch (Exception e) {
            throw new RuntimeException("JSON 파싱 실패");
        }

    }

    @Transactional
    public void syncStockInfo() {
        String apiURL = "https://apis.data.go.kr/1160100/service/GetStockSecuritiesInfoService/getStockPriceInfo?serviceKey=5804bce7ab0848dd3213a6f1c09808866b12191c667458304f0d6f10a900a6b4&numOfRows=3500&resultType=json";
        String jsonResult = restTemplate.getForObject(apiURL, String.class);

        List<StockInfoRes> stockInfoList = parseJson(jsonResult);

        HashMap<String, String> stockIdMap = new HashMap<>();
        for (StockInfoRes stockInfo : stockInfoList) {
            stockIdMap.put(stockInfo.getStockId(), stockInfo.getStockName());
        }

        stockInfoList.clear();

        for (String stockId : stockIdMap.keySet()) {
            stockInfoList.add(StockInfoRes.builder().stockId(stockId).stockName(stockIdMap.get(stockId)).build());
        }

        // stockInfoList에 담긴 정보를 바탕으로 STOCKS 테이블에 관련 정보를 저장한다.
        // 이때, stockInfoList에 담긴 종목이 테이블에 이미 존재하면 건너뛰고, 존재하지 않는다면 삽입한다.
        // 단, stockInfoList에 담겨있지 않지만 STOCK 테이블에 존재한다면 IS_ACTIVE를 N으로 바꿔야 한다.

        // 스케쥴러가 작동하는 시각이 장이 열리기 전인 오전 8시 30분이므로, 먼저 STOCKS 테이블에 존재하는 모든 종목들의 IS_ACTIVE 값을 N으로 바꾼다.
        // 그 다음, 조회된 데이터들에 대해서 UPSERT를 수행한다. 이 경우, IS_ACTIVE 값 또한 Y로 바뀌도록 한다.

        log.info("===Start Update IS_ACTIVE to N===");
        stockInfoMapper.updateStockInfoIsActiveToN();
        log.info("===End Update IS_ACTIVE to N===");
        
        log.info("===Start Upsert STOCKS===");
        stockInfoMapper.upsertStockInfo(stockInfoList);
        log.info("===End Upsert STOCKS===");
    }
}
