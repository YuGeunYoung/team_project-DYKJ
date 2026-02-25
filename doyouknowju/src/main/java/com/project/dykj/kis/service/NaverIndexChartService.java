package com.project.dykj.kis.service;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
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
import com.project.dykj.kis.KisProperties;

@Service
public class NaverIndexChartService {

    private static final Logger log = LoggerFactory.getLogger(NaverIndexChartService.class);
    private static final int MIN_INITIAL_POINTS = 120;
    private static final int MAX_BACKFILL_PAGES = 8;
    private static final int ONE_HOUR_LOOKBACK_DAYS = 3;

    private final KisProperties.NaverIndexChart properties;
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final ConcurrentMap<String, Cache> cacheByKey = new ConcurrentHashMap<>();

    /** 네이버 지수 차트 API 호출용 클라이언트 초기화 */
    public NaverIndexChartService(KisProperties kisProperties) {
        this.properties = kisProperties.getNaverIndexChart();
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
        return getOrFetch("KOSPI", normalizeRange(range), normalizeDateTime(startDateTime),
                normalizeDateTime(endDateTime));
    }

    public Map<String, Object> getKosdaqChart(String range, String startDateTime, String endDateTime) {
        return getOrFetch("KOSDAQ", normalizeRange(range), normalizeDateTime(startDateTime),
                normalizeDateTime(endDateTime));
    }

    /**
     * 시장/범위/기간 조합별 캐시를 우선 사용하고,
     * 캐시 미스 시 네이버 API를 호출해 표준 응답 형태로 반환한다.
     */
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
                List<Map<String, Object>> points = fetchPointsWithBackfill(requestUrl, range);
                if (points.isEmpty()) {
                    return null;
                }
                points = normalizePointsByRange(range, points);
                if (points.isEmpty()) {
                    return null;
                }

                Map<String, Object> normalized = Map.of(
                        "rt_cd", "0",
                        "msg_cd", "MCA00000",
                        "msg1", "정상처리 되었습니다.",
                        "range", range,
                        "output", points);

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

    /** 1h 범위는 시간 API에 대해 멀티데이 백필을 수행하고, 그 외는 단일 호출 처리 */
    private List<Map<String, Object>> fetchPointsWithBackfill(String requestUrl, String range) throws Exception {
        if ("1h".equals(range) && isTimeApiUrl(requestUrl)) {
            return fetchOneHourMultiDayPoints(requestUrl);
        }
        return fetchSingleRequestPoints(requestUrl);
    }

    /** 시간 API에서 최근 N일 데이터를 합쳐 1분 시계열을 확보한다. */
    private List<Map<String, Object>> fetchOneHourMultiDayPoints(String requestUrl) throws Exception {
        Map<String, Map<String, Object>> merged = new LinkedHashMap<>();
        LocalDate today = LocalDate.now();
        for (int offset = 0; offset < ONE_HOUR_LOOKBACK_DAYS; offset++) {
            LocalDate target = today.minusDays(offset);
            String dayUrl = upsertQueryParam(
                    requestUrl,
                    "thistime",
                    target.format(DateTimeFormatter.BASIC_ISO_DATE));
            List<Map<String, Object>> dayPoints = fetchSingleRequestPoints(dayUrl);
            if (!dayPoints.isEmpty()) {
                mergePoints(merged, dayPoints);
            }
        }
        List<Map<String, Object>> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(this::extractSortableTime));
        return result;
    }

    /** 단일 URL 응답을 파싱하고, 필요하면 page 기반으로 추가 백필한다. */
    private List<Map<String, Object>> fetchSingleRequestPoints(String requestUrl) throws Exception {
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

        List<Map<String, Object>> result = new ArrayList<>(merged.values());
        result.sort(Comparator.comparing(this::extractSortableTime));
        return result;
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

    /** thistime 기준으로 중복 포인트를 제거하면서 첫 등장 포인트를 유지한다. */
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

    /** range/시장에 맞는 네이버 요청 URL을 동적으로 생성한다. */
    private String buildRequestUrl(String market, String range, String startDateTime, String endDateTime) {
        String base = properties.getApiBaseUrl() + "/" + market + "/" + toPathRange(range);
        String withEnd = upsertQueryParam(base, "endDateTime", endDateTime == null ? nowDateTime() : endDateTime);
        if (startDateTime != null) {
            return upsertQueryParam(withEnd, "startDateTime", startDateTime);
        }
        return upsertQueryParam(withEnd, "startDateTime", defaultStartDateTime(range));
    }

    /** 1h 범위는 장중(09:00~15:30)과 시간 범위를 필터링해 노이즈를 제거한다. */
    private List<Map<String, Object>> normalizePointsByRange(String range, List<Map<String, Object>> points) {
        if (!"1h".equals(range)) {
            return points;
        }
        LocalDate today = LocalDate.now();
        LocalDate startDate = today.minusDays(ONE_HOUR_LOOKBACK_DAYS - 1);
        LocalTime close = LocalTime.of(15, 30, 0);
        LocalTime now = LocalTime.now();
        return points.stream()
                .filter(point -> {
                    LocalDateTime dateTime = parsePointDateTime(point);
                    if (dateTime == null) {
                        return false;
                    }
                    LocalDate date = dateTime.toLocalDate();
                    LocalTime time = dateTime.toLocalTime();
                    if (date.isBefore(startDate) || date.isAfter(today)) {
                        return false;
                    }
                    if (time.isBefore(LocalTime.of(9, 0)) || time.isAfter(close)) {
                        return false;
                    }
                    if (date.equals(today) && time.isAfter(now)) {
                        return false;
                    }
                    return true;
                })
                .toList();
    }

    private String extractSortableTime(Map<String, Object> point) {
        LocalDateTime dateTime = parsePointDateTime(point);
        return dateTime == null ? "00000000000000" : dateTime.format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
    }

    private LocalDateTime parsePointDateTime(Map<String, Object> point) {
        if (point == null) {
            return null;
        }
        Object raw = point.get("thistime");
        if (raw == null) {
            raw = point.get("dateTime");
        }
        if (raw == null) {
            raw = point.get("localDateTime");
        }
        if (raw == null) {
            raw = point.get("localTradedAt");
        }
        if (raw == null) {
            return null;
        }
        String rawText = String.valueOf(raw).trim();
        if (rawText.isEmpty()) {
            return null;
        }
        try {
            return java.time.OffsetDateTime.parse(rawText).toLocalDateTime();
        } catch (Exception ignore) {
        }
        try {
            return java.time.ZonedDateTime.parse(rawText).toLocalDateTime();
        } catch (Exception ignore) {
        }
        try {
            return LocalDateTime.parse(rawText);
        } catch (Exception ignore) {
        }

        String digits = rawText.replaceAll("[^0-9]", "");
        try {
            if (digits.length() >= 14) {
                return LocalDateTime.parse(digits.substring(0, 14), DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
            }
            if (digits.length() == 12) {
                return LocalDateTime.parse(digits, DateTimeFormatter.ofPattern("yyyyMMddHHmm"));
            }
        } catch (Exception ignore) {
            return null;
        }
        return null;
    }

    private String defaultStartDateTime(String range) {
        LocalDate now = LocalDate.now();
        if ("1h".equals(range)) {
            LocalDate from = now.minusDays(ONE_HOUR_LOOKBACK_DAYS - 1L);
            return from.format(DateTimeFormatter.BASIC_ISO_DATE) + "0900";
        }
        LocalDate from = switch (range) {
            case "week" -> now.minusYears(2);
            case "month" -> now.minusYears(10);
            case "year" -> now.minusYears(20);
            default -> now.minusMonths(6);
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

    /** 서비스 내부 range 값을 네이버 chart API path 값으로 변환한다. */
    private String toPathRange(String range) {
        return switch (range) {
            case "1h" -> "minute10";
            case "week" -> "week";
            case "month" -> "month";
            case "year" -> "year";
            default -> "day";
        };
    }

    private String ensureTodayForTimeApi(String url) {
        if (url == null || url.isBlank()) {
            return url;
        }
        String today = LocalDate.now().format(DateTimeFormatter.BASIC_ISO_DATE);
        return upsertQueryParam(url, "thistime", today);
    }

    /** 네이버 응답 포맷(list/map)을 공통 list-of-map 형태로 정규화한다. */
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
