package com.project.dykj.kis.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

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
    private static final int MIN_INITIAL_POINTS = 120;
    private static final int MAX_BACKFILL_PAGES = 8;

    private final NaverIndexChartProperties properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Cache> cacheByKey = new ConcurrentHashMap<>();

    public NaverIndexChartService(NaverIndexChartProperties properties) {
        this.properties = properties;
        this.webClient = WebClient.builder()
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT, "Mozilla/5.0")
                .build();
        this.objectMapper = new ObjectMapper();
    }

    public Map<String, Object> getKospiChart() {
        return getKospiChart(properties.getDefaultRange(), null, null);
    }

    public Map<String, Object> getKosdaqChart() {
        return getKosdaqChart(properties.getDefaultRange(), null, null);
    }

    public Map<String, Object> getKospiChart(String range) {
        return getKospiChart(range, null, null);
    }

    public Map<String, Object> getKosdaqChart(String range) {
        return getKosdaqChart(range, null, null);
    }

    public Map<String, Object> getKospiChart(String range, String startDateTime, String endDateTime) {
        return getOrFetch("KOSPI", normalizeRange(range), normalizeDateTime(startDateTime), normalizeDateTime(endDateTime));
    }

    public Map<String, Object> getKosdaqChart(String range, String startDateTime, String endDateTime) {
        return getOrFetch("KOSDAQ", normalizeRange(range), normalizeDateTime(startDateTime), normalizeDateTime(endDateTime));
    }

    public boolean isKospiConfigured() {
        return isConfigured(properties.getApiBaseUrl()) || isConfigured(properties.getKospiUrl());
    }

    public boolean isKosdaqConfigured() {
        return isConfigured(properties.getApiBaseUrl()) || isConfigured(properties.getKosdaqUrl());
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getOrFetch(String market, String range, String startDateTime, String endDateTime) {
        String cacheKey = market + ":" + range + ":" + nullToEmpty(startDateTime) + ":" + nullToEmpty(endDateTime);
        Cache current = cacheByKey.getOrDefault(cacheKey, Cache.empty());
        if (current.isValid()) {
            return current.value;
        }

        synchronized (cacheKey.intern()) {
            current = cacheByKey.getOrDefault(cacheKey, Cache.empty());
            if (current.isValid()) {
                return current.value;
            }

            String requestUrl = buildRequestUrl(market, range, startDateTime, endDateTime);
            if (!isConfigured(requestUrl)) {
                return null;
            }

            try {
                List<Map<String, Object>> points = fetchPointsWithBackfill(requestUrl);
                if (points.isEmpty()) {
                    return null;
                }

                Map<String, Object> normalized = Map.of(
                        "rt_cd", "0",
                        "msg_cd", "MCA00000",
                        "msg1", "정상처리 되었습니다.",
                        "range", range,
                        "output", points
                );

                cacheByKey.put(cacheKey, new Cache(normalized, Instant.now().plus(cacheTtl())));
                return normalized;
            } catch (WebClientResponseException e) {
                log.warn("NAVER index chart failed: market={} range={} status={} url={} body={}",
                        market, range, e.getRawStatusCode(), requestUrl, truncate(e.getResponseBodyAsString()));
                return current.value;
            } catch (Exception e) {
                log.warn("NAVER index chart parse failed: market={} range={} url={} msg={}",
                        market, range, requestUrl, e.getMessage());
                return current.value;
            }
        }
    }

    private List<Map<String, Object>> fetchPointsWithBackfill(String requestUrl) throws Exception {
        String firstJson = fetchJson(requestUrl);
        if (firstJson == null || firstJson.isBlank()) {
            return List.of();
        }

        Object firstParsed = objectMapper.readValue(firstJson, Object.class);
        List<Map<String, Object>> firstPoints = extractPoints(firstParsed);
        if (firstPoints.isEmpty()) {
            return List.of();
        }

        if (!isTimeApiUrl(requestUrl) || firstPoints.size() >= MIN_INITIAL_POINTS) {
            return firstPoints;
        }

        int pageSize = extractIntQueryParam(requestUrl, "pageSize", firstPoints.size());
        if (pageSize <= 0) {
            pageSize = firstPoints.size();
        }

        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        mergePoints(merged, firstPoints);

        for (int page = 1; page < MAX_BACKFILL_PAGES && merged.size() < MIN_INITIAL_POINTS; page++) {
            int startIdx = page * pageSize;
            String nextUrl = upsertQueryParam(requestUrl, "startIdx", String.valueOf(startIdx));
            try {
                String nextJson = fetchJson(nextUrl);
                if (nextJson == null || nextJson.isBlank()) {
                    break;
                }
                Object nextParsed = objectMapper.readValue(nextJson, Object.class);
                List<Map<String, Object>> nextPoints = extractPoints(nextParsed);
                if (nextPoints.isEmpty()) {
                    break;
                }
                int before = merged.size();
                mergePoints(merged, nextPoints);
                if (merged.size() == before) {
                    break;
                }
            } catch (Exception pageError) {
                log.warn("NAVER index backfill failed: url={} msg={}", nextUrl, pageError.getMessage());
                break;
            }
        }

        return new ArrayList<>(merged.values());
    }

    private String fetchJson(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(String.class)
                .block(timeout());
    }

    private boolean isTimeApiUrl(String url) {
        return url != null && url.contains("/indexSise/time");
    }

    private int extractIntQueryParam(String url, String name, int fallback) {
        if (url == null) {
            return fallback;
        }
        String pattern = name + "=(\\d+)";
        java.util.regex.Matcher matcher = java.util.regex.Pattern.compile(pattern).matcher(url);
        if (!matcher.find()) {
            return fallback;
        }
        try {
            return Integer.parseInt(matcher.group(1));
        } catch (NumberFormatException ignore) {
            return fallback;
        }
    }

    private String upsertQueryParam(String url, String name, String value) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String pattern = name + "=\\d+";
        if (url.matches(".*" + pattern + ".*")) {
            return url.replaceFirst(pattern, name + "=" + value);
        }
        String separator = url.contains("?") ? "&" : "?";
        return url + separator + name + "=" + value;
    }

    private void mergePoints(Map<String, Map<String, Object>> merged, List<Map<String, Object>> points) {
        for (int index = 0; index < points.size(); index++) {
            Map<String, Object> point = points.get(index);
            if (point == null || point.isEmpty()) {
                continue;
            }
            Object thistime = point.get("thistime");
            String key = thistime == null ? "idx-" + merged.size() + "-" + index : String.valueOf(thistime);
            merged.putIfAbsent(key, point);
        }
    }

    private String buildRequestUrl(String market, String range, String startDateTime, String endDateTime) {
        if (isConfigured(properties.getApiBaseUrl())) {
            String base = properties.getApiBaseUrl() + "/" + market + "/" + toPathRange(range);
            String withEnd = upsertQueryParam(base, "endDateTime", endDateTime == null ? nowDateTime() : endDateTime);
            if (startDateTime != null) {
                return upsertQueryParam(withEnd, "startDateTime", startDateTime);
            }
            return upsertQueryParam(withEnd, "startDateTime", defaultStartDateTime(range));
        }

        String fallback = "KOSDAQ".equals(market) ? properties.getKosdaqUrl() : properties.getKospiUrl();
        return normalizeDateParam(fallback);
    }

    private String defaultStartDateTime(String range) {
        LocalDate now = LocalDate.now();
        if ("1h".equals(range)) {
            return now.format(DateTimeFormatter.BASIC_ISO_DATE) + "0900";
        }
        LocalDate from = switch (range) {
            case "week" -> now.minusYears(1);
            case "month" -> now.minusYears(5);
            case "year" -> now.minusYears(10);
            default -> now.minusMonths(3);
        };
        return from.format(DateTimeFormatter.BASIC_ISO_DATE) + "0000";
    }

    private String nowDateTime() {
        return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
    }

    private String normalizeDateTime(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.length() == 8) {
            return digits + "0000";
        }
        if (digits.length() == 12) {
            return digits;
        }
        if (digits.length() >= 12) {
            return digits.substring(0, 12);
        }
        return null;
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    private String normalizeRange(String range) {
        if (range == null || range.isBlank()) {
            return normalizeRange(properties.getDefaultRange());
        }

        String value = range.trim().toLowerCase();
        return switch (value) {
            case "hour", "h", "1h" -> "1h";
            case "day", "d", "1d" -> "day";
            case "week", "w", "1w" -> "week";
            case "month", "m", "1m" -> "month";
            case "year", "y", "1y" -> "year";
            default -> "day";
        };
    }

    private String toPathRange(String range) {
        return switch (range) {
            case "1h" -> "day";
            case "week" -> "week";
            case "month" -> "month";
            case "year" -> "year";
            default -> "day";
        };
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
            return List.of(objectMapper.convertValue(map, new TypeReference<Map<String, Object>>() {}));
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
