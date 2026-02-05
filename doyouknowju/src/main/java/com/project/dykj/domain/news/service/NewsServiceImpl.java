package com.project.dykj.domain.news.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import com.project.dykj.domain.news.mapper.NewsMapper;
import com.project.dykj.domain.news.vo.NewsVO;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class NewsServiceImpl implements NewsService {

    @Autowired
    private NewsMapper newsMapper;

    @Value("${gemini.api.key}")
    private String geminiKey;

    @Override
    public List<NewsVO> getLatestNews() {
        return newsMapper.selectLatestNews();
    }

    @Override
    public void refreshNews() {
        // 1. 네이버 금융 뉴스 크롤링 (JSoup)
        List<NewsVO> newsList = fetchNaverFinanceNews();

        // 2. 코스피/코스닥 지수 크롤링
        String marketIndex = fetchMarketIndex();

        // 3. 뉴스 제목 + 증시 지수 합쳐서 Gemini 요약 요청
        StringBuilder sb = new StringBuilder();
        sb.append("오늘(2026년 2월 4일) 한국 증시(코스피, 코스닥) 마감 시황과 관련 주요 뉴스를 구글 검색을 통해 확인하세요. ");
        sb.append("확인된 내용을 바탕으로 핵심 이슈와 등락 여부를 포함하여 공백 포함 50자 이내로 요약해 주세요. ");
        sb.append("이 외 다른 내용은 포함하지 마세요.\n\n");
        sb.append("[시장 지수]\n").append(marketIndex).append("\n\n");
        sb.append("[주요 뉴스]\n");
        for (NewsVO vo : newsList)
            sb.append("- ").append(vo.getTitle()).append("\n");

        String summary = fetchGeminiSummary(sb.toString());

        // 4. 모든 뉴스 객체에 요약 정보 저장 (단순화를 위해 모든 row에 저장 또는 별도 관리)
        for (NewsVO vo : newsList) {
            vo.setAiSummary(summary);
            vo.setNewsCategory("경제"); // 카테고리 고정
        }

        // 5. DB 저장
        if (!newsList.isEmpty()) {
            // 편의상 기존 데이터 삭제 후 삽입하거나, 중복 체크 로직이 필요할 수 있음.
            // 여기선 Mapper 로직(Sequence 사용) 그대로 사용
            try {
                newsMapper.insertNewsList(newsList);
                log.info("뉴스 {}건 갱신 완료", newsList.size());
            } catch (Exception e) {
                log.error("뉴스 DB 저장 실패", e);
            }
        }
    }

    // 네이버 금융 > 뉴스 > 주요뉴스 > 경제 크롤링
    private List<NewsVO> fetchNaverFinanceNews() {
        List<NewsVO> list = new ArrayList<>();
        // 네이버 금융 뉴스 (경제 파트)
        String url = "https://finance.naver.com/news/mainnews.naver";

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .get();

            // 뉴스 리스트 셀렉터 (네이버 금융 구조에 맞춤)
            Elements articles = doc.select(".newsList .block1");

            for (Element article : articles) {
                NewsVO vo = new NewsVO();

                // 제목 & 링크
                Element titleEl = article.selectFirst("dt.articleSubject a");
                if (titleEl == null)
                    titleEl = article.selectFirst("dd.articleSubject a"); // 썸네일 없는 경우 구조가 다를 수 있음

                if (titleEl != null) {
                    vo.setTitle(titleEl.text());
                    vo.setNewsUrl("https://finance.naver.com" + titleEl.attr("href"));
                }

                // 썸네일
                Element imgEl = article.selectFirst("dt.thumb a img");
                if (imgEl != null) {
                    vo.setImageUrl(imgEl.attr("src"));
                }

                // 날짜 및 신문사 (네이버 금융은 wdate 등에 날짜가 있음)
                Element dateEl = article.selectFirst("dd.articleSummary .wdate");
                if (dateEl != null) {
                    vo.setPubDate(dateEl.text());
                }

                // 기사 요약 내용 (필요시)
                // Element summaryEl = article.selectFirst("dd.articleSummary");
                // if(summaryEl != null) String summaryText = summaryEl.ownText();

                if (vo.getTitle() != null && !vo.getTitle().isEmpty()) {
                    list.add(vo);
                }

                if (list.size() >= 10)
                    break; // 최대 10개만
            }

        } catch (IOException e) {
            log.error("네이버 금융 뉴스 크롤링 실패", e);
        }

        return list;
    }

    // 네이버 금융 메인에서 코스피/코스닥 지수 가져오기
    private String fetchMarketIndex() {
        String url = "https://finance.naver.com/";
        StringBuilder idxInfo = new StringBuilder();
        try {
            Document doc = Jsoup.connect(url).get();
            // 코스피
            Element kospiNow = doc.selectFirst(".kospi_area .num_quot .num");
            Element kospiRate = doc.selectFirst(".kospi_area .num_quot .num2");
            Element kospiPer = doc.selectFirst(".kospi_area .num_quot .num3");

            // 코스닥
            Element kosdaqNow = doc.selectFirst(".kosdaq_area .num_quot .num");
            Element kosdaqRate = doc.selectFirst(".kosdaq_area .num_quot .num2");
            Element kosdaqPer = doc.selectFirst(".kosdaq_area .num_quot .num3");

            if (kospiNow != null)
                idxInfo.append("코스피: ").append(kospiNow.text()).append(" (").append(kospiRate.text()).append(" ")
                        .append(kospiPer.text()).append(")\n");
            if (kosdaqNow != null)
                idxInfo.append("코스닥: ").append(kosdaqNow.text()).append(" (").append(kosdaqRate.text()).append(" ")
                        .append(kosdaqPer.text()).append(")");

        } catch (Exception e) {
            log.error("증시 지수 크롤링 실패", e);
            return "증시 정보 로드 실패";
        }
        return idxInfo.toString();
    }

    private String fetchGeminiSummary(String prompt) {
        // 모델 변경: gemini-pro (안정성)
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-pro:generateContent?key="
                + geminiKey;

        RestTemplate rt = new RestTemplate();

        // 헤더 설정
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");

        // 바디 설정
        Map<String, Object> body = Map.of("contents", List.of(Map.of("parts", List.of(Map.of("text", prompt)))));
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        try {
            // API 호출
            ResponseEntity<Map> resp = rt.exchange(url, HttpMethod.POST, entity, Map.class);
            Map<String, Object> respBody = resp.getBody();

            if (respBody != null && respBody.containsKey("candidates")) {
                List<Map> candidates = (List<Map>) respBody.get("candidates");
                if (candidates != null && !candidates.isEmpty()) {
                    Map content = (Map) candidates.get(0).get("content");
                    List<Map> parts = (List<Map>) content.get("parts");
                    return (String) parts.get(0).get("text");
                }
            }
        } catch (HttpClientErrorException e) {
            log.error("Gemini API 호출 오류: Code={}, Body={}", e.getStatusCode(), e.getResponseBodyAsString());
        } catch (Exception e) {
            log.error("Gemini 요약 요청 실패 (예외 발생)", e);
        }
        return "AI 요약 생성에 실패했습니다.";
    }
}