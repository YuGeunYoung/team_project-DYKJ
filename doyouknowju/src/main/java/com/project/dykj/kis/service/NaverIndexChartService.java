package com.project.dykj.kis.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.project.dykj.kis.NaverIndexChartProperties;

@Service
public class NaverIndexChartService {

    private static final Logger log = LoggerFactory.getLogger(NaverIndexChartService.class);

    private final NaverIndexChartProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private volatile Cache kospiCache = Cache.empty();
    private volatile Cache kosdaqCache = Cache.empty();

    public NaverIndexChartService(NaverIndexChartProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> getKospiChart() {
        return getOrFetch("kospi");
    }

    public Map<String, Object> getKosdaqChart() {
        return getOrFetch("kosdaq");
    }

    public boolean isKospiConfigured() {
        return isConfigured(properties.getKospiUrl());
    }

    public boolean isKosdaqConfigured() {
        return isConfigured(properties.getKosdaqUrl());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrFetch(String market) {
        Cache current = "kosdaq".equals(market) ? kosdaqCache : kospiCache;
        if (current.isValid()) {
            return current.value;
        }

        synchronized (this) {
            current = "kosdaq".equals(market) ? kosdaqCache : kospiCache;
            if (current.isValid()) {
                return current.value;
            }
            String url = "kosdaq".equals(market) ? properties.getKosdaqUrl() : properties.getKospiUrl();
            if (!isConfigured(url)) {
                return null;
            }
            String requestUrl = normalizeDateParam(url);

            try {
                String json = webClient.get()
                        .uri(requestUrl)
                        .retrieve()
                        .bodyToMono(String.class)
                        .block(timeout());

                if (json == null || json.isBlank()) {
                    return null;
                }

                Object parsed = objectMapper.readValue(json, Object.class);
                List<Map<String, Object>> points = extractPoints(parsed);
                Map<String, Object> normalized = Map.of(
                        "rt_cd", "0",
                        "msg_cd", "MCA00000",
                        "msg1", "정상처리 되었습니다.",
                        "output", points
                );

                Cache next = new Cache(normalized, Instant.now().plus(cacheTtl()));
                if ("kosdaq".equals(market)) {
                    kosdaqCache = next;
                } else {
                    kospiCache = next;
                }
                return normalized;
            } catch (WebClientResponseException e) {
                log.warn("NAVER index chart failed: market={} status={} url={} body={}",
                        market, e.getRawStatusCode(), requestUrl, truncate(e.getResponseBodyAsString()));
                return current.value;
            } catch (Exception e) {
                log.warn("NAVER index chart parse failed: market={} url={} msg={}", market, requestUrl, e.getMessage());
                return current.value;
            }
        }
    }

    private String normalizeDateParam(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return url.replaceAll("thistime=\\d{8}", "thistime=" + today);
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractPoints(Object parsed) {
        if (parsed == null) {
            return List.of();
        }
        if (parsed instanceof List<?> list) {
            return list.stream()
                    .filter(v -> v instanceof Map<?, ?>)
                    .map(v -> (Map<String, Object>) v)
                    .toList();
        }
        if (parsed instanceof Map<?, ?> map) {
            for (String key : List.of("output", "data", "items", "results", "result", "chart", "output2")) {
                Object raw = map.get(key);
                if (raw instanceof List<?> list) {
                    return list.stream()
                            .filter(v -> v instanceof Map<?, ?>)
                            .map(v -> (Map<String, Object>) v)
                            .toList();
                }
            }
            return List.of(objectMapper.convertValue(map, new TypeReference<Map<String, Object>>() {
            }));
        }
        return List.of();
    }

    private boolean isConfigured(String url) {
        return url != null && !url.isBlank();
    }

    private Duration timeout() {
        Duration t = properties.getTimeout();
        return t == null ? Duration.ofSeconds(5) : t;
    }

    private Duration cacheTtl() {
        Duration ttl = properties.getCacheTtl();
        return ttl == null ? Duration.ofSeconds(20) : ttl;
    }

    private String truncate(String body) {
        if (body == null) {
            return "";
        }
        return body.length() > 400 ? body.substring(0, 400) + "...(truncated)" : body;
    }

    private record Cache(Map<String, Object> value, Instant expiresAt) {
        static Cache empty() {
            return new Cache(null, Instant.EPOCH);
        }

        boolean isValid() {
            return value != null && Objects.nonNull(expiresAt) && Instant.now().isBefore(expiresAt);
        }
    }
}
