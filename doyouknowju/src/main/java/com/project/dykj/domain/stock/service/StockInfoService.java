package com.project.dykj.domain.stock.service;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.JsonObject;
import com.project.dykj.domain.stock.dto.res.StockInfoRes;
import com.project.dykj.domain.stock.mapper.StockInfoMapper;

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

    public void syncStockInfo() {
        String apiURL = "https://apis.data.go.kr/1160100/service/GetStockSecuritiesInfoService/getStockPriceInfo?serviceKey=5804bce7ab0848dd3213a6f1c09808866b12191c667458304f0d6f10a900a6b4&numOfRows=3500&resultType=json";
        String jsonResult = restTemplate.getForObject(apiURL, String.class);

        List<StockInfoRes> stockInfoList = parseJson(jsonResult);

        for (StockInfoRes stockInfo : stockInfoList) {
            log.info(stockInfo.toString());
        }
    }
}
