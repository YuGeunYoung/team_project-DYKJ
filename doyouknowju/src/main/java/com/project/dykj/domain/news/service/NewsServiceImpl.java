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

    @Value("${naver.client.id}")
    private String naverClientId;

    @Value("${naver.client.secret}")
    private String naverClientSecret;

    @Override
    public List<NewsVO> getLatestNews() {
        return newsMapper.selectLatestNews();
    }

    @Override
    public List<NewsVO> searchNews(String keyword) {
        List<NewsVO> list = new ArrayList<>();
        try {
            String url = "https://openapi.naver.com/v1/search/news.json?query="
                    + java.net.URLEncoder.encode("\"" + keyword + "\"", "UTF-8") + "&display=10&sort=date";

            RestTemplate rt = new RestTemplate();
            HttpHeaders headers = new HttpHeaders();
            headers.set("X-Naver-Client-Id", naverClientId);
            headers.set("X-Naver-Client-Secret", naverClientSecret);

            HttpEntity<String> entity = new HttpEntity<>(headers);
            ResponseEntity<Map> response = rt.exchange(url, HttpMethod.GET, entity, Map.class);

            Map<String, Object> body = response.getBody();
            if (body != null && body.containsKey("items")) {
                List<Map<String, Object>> items = (List<Map<String, Object>>) body.get("items");
                for (Map<String, Object> item : items) {
                    NewsVO vo = new NewsVO();
                    // 네이버 API는 HTML 태그가 포함되어 올 수 있으므로 제거 필요
                    String title = (String) item.get("title");
                    String link = (String) item.get("link"); // 원본 링크 or 네이버 뉴스 링크
                    String pubDate = (String) item.get("pubDate");

                    vo.setTitle(title.replaceAll("<[^>]*>", "").replaceAll("&quot;", "\"").replaceAll("&amp;", "&"));
                    vo.setNewsUrl(link);
                    vo.setPubDate(pubDate); // 형식이 다를 수 있으나 우선 그대로 저장
                    vo.setNewsCategory("검색");

                    // 이미지는 API에서 주지 않음 -> 비워두거나 기본 이미지 처리 프론트에서
                    list.add(vo);
                }
            }
        } catch (Exception e) {
            log.error("네이버 뉴스 검색 API 호출 실패: {}", e.getMessage());
        }
        return list;
    }

    @Override
    public void refreshNews() {
        log.info("뉴스 및 증시 정보 갱신 프로세스 시작");

        // 1. 네이버 금융 뉴스 크롤링 (JSoup)
        List<NewsVO> newsList = fetchNaverFinanceNews();
        log.info("뉴스 가져오기 완료: {}건", newsList.size());

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

        log.info("Gemini 요약 요청 전송... (Input Length: {})", sb.length());
        String summary = fetchGeminiSummary(sb.toString());
        log.info("Gemini 요약 응답 수신: {}", summary);

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
                log.info("DB 저장 성공: 뉴스 {}건 갱신 완료", newsList.size());
            } catch (Exception e) {
                log.error("뉴스 DB 저장 실패", e);
            }
        } else {
            log.warn("뉴스 리스트가 비어 있어 DB 저장을 건너뜁니다.");
        }

        log.info("갱신 프로세스 종료");
    }

    // 네이버 금융 > 뉴스 > 주요뉴스 > 경제 크롤링
    private List<NewsVO> fetchNaverFinanceNews() {
        List<NewsVO> list = new ArrayList<>();
        // 네이버 금융 뉴스 (경제 파트)
        String url = "https://finance.naver.com/news/mainnews.naver";

        log.info("네이버 뉴스 크롤링 시작: {}", url);

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent(
                            "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                    .get();

            // 뉴스 리스트 셀렉터 (네이버 금융 구조에 맞춤)
            Elements articles = doc.select(".newsList .block1");
            log.info("크롤링 원본 개수 (block1): {}", articles.size());

            if (articles.isEmpty()) {
                log.warn("뉴스 리스트를 찾을 수 없습니다. CSS 셀렉터(.newsList .block1)를 확인해주세요.");
            }

            for (Element article : articles) {
                NewsVO vo = new NewsVO();

                // 제목 & 링크
                Element titleEl = article.selectFirst("dt.articleSubject a");
                if (titleEl == null)
                    titleEl = article.selectFirst("dd.articleSubject a"); // 썸네일 없는 경우 구조가 다를 수 있음

                if (titleEl != null) {
                    vo.setTitle(titleEl.text());
                    vo.setNewsUrl("https://finance.naver.com" + titleEl.attr("href"));
                } else {
                    log.warn("기사 제목 요소를 찾을 수 없음: {}", article.html());
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

                if (vo.getTitle() != null && !vo.getTitle().isEmpty()) {
                    list.add(vo);
                }

                if (list.size() >= 10)
                    break; // 최대 10개만
            }

            log.info("최종 파싱된 뉴스 개수: {}", list.size());

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

            log.info("증시 지수 크롤링 결과: {}", idxInfo.toString());

        } catch (Exception e) {
            log.error("증시 지수 크롤링 실패", e);
            return "증시 정보 로드 실패";
        }
        return idxInfo.toString();
    }

    private String fetchGeminiSummary(String prompt) {
        // 모델 변경: gemini-flash
        String url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-3-flash-preview:generateContent?key="
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