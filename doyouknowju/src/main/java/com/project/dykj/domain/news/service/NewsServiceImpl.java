package com.project.dykj.domain.news.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import com.project.dykj.domain.news.mapper.NewsMapper;
import com.project.dykj.domain.news.vo.NewsVO;

@Service
public class NewsServiceImpl implements NewsService {

    @Autowired
    private NewsMapper newsMapper;

    @Value("${naver.client.id}") private String naverId;
    @Value("${naver.client.secret}") private String naverSecret;
    @Value("${gemini.api.key}") private String geminiKey;

    @Override
    public List<NewsVO> getLatestNews() {
        return newsMapper.selectLatestNews();
    }

    @Override
    public void refreshNews() {
        List<NewsVO> newsList = fetchNaverNews();
        
        StringBuilder sb = new StringBuilder();
        sb.append("오늘(2026년 2월 4일) 한국 증시(코스피, 코스닥) 마감 시황과 관련 주요 뉴스를 구글 검색을 통해 확인하세요. ");
        sb.append("확인된 내용을 바탕으로 핵심 이슈와 등락 여부를 포함하여 공백 포함 50자 이내로 요약해 주세요. ");
        sb.append("이 외 다른 내용은 포함하지 마세요.\n주요 뉴스 제목들:\n");
        for(NewsVO vo : newsList) sb.append("- ").append(vo.getTitle()).append("\n");

        String summary = fetchGeminiSummary(sb.toString());
        for(NewsVO vo : newsList) {
            vo.setAiSummary(summary);
            vo.setNewsCategory("경제");
        }
        
        if (!newsList.isEmpty()) {
            newsMapper.insertNewsList(newsList);
        }
    }

    private List<NewsVO> fetchNaverNews() {
        String url = "https://openapi.naver.com/v1/search/news.json?query=주식시황&display=10&sort=sim";
        RestTemplate rt = new RestTemplate();
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Naver-Client-Id", naverId);
        headers.set("X-Naver-Client-Secret", naverSecret);

        try {
            ResponseEntity<Map> resp = rt.exchange(url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            List<Map<String, String>> items = (List<Map<String, String>>) resp.getBody().get("items");
            List<NewsVO> list = new ArrayList<>();
            if (items != null) {
                for (Map<String, String> item : items) {
                    NewsVO vo = new NewsVO();
                    vo.setTitle(item.get("title").replaceAll("<[^>]*>", ""));
                    vo.setNewsUrl(item.get("link"));
                    vo.setPubDate(item.get("pubDate"));
                    list.add(vo);
                }
            }
            return list;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String fetchGeminiSummary(String prompt) {
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + geminiKey;
        RestTemplate rt = new RestTemplate();
        Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        try {
            ResponseEntity<Map> resp = rt.postForEntity(url, body, Map.class);
            List<Map> candidates = (List<Map>) resp.getBody().get("candidates");
            Map content = (Map) candidates.get(0).get("content");
            List<Map> parts = (List<Map>) content.get("parts");
            return (String) parts.get(0).get("text");
        } catch (Exception e) {
            return "요약 생성 실패";
        }
    }
}