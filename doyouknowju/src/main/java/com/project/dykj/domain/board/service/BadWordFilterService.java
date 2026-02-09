package com.project.dykj.domain.board.service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

@Service
public class BadWordFilterService {

    private final List<Pattern> badWordPatterns;

    public BadWordFilterService() {
        this.badWordPatterns = loadBadWordPatterns();
    }

    public String mask(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }

        String masked = text;
        for (Pattern pattern : badWordPatterns) {
            masked = pattern.matcher(masked).replaceAll(this::toMask);
        }
        return masked;
    }

    private List<Pattern> loadBadWordPatterns() {
        ClassPathResource resource = new ClassPathResource("badwords.txt");
        if (!resource.exists()) {
            return List.of();
        }

        try (InputStream inputStream = resource.getInputStream();
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {

            List<String> badWords = reader.lines()
                    .map(String::trim)
                    .filter(line -> !line.isEmpty())
                    .filter(line -> !line.startsWith("#"))
                    .collect(Collectors.toList());

            List<Pattern> patterns = new ArrayList<>();
            for (String badWord : badWords) {
                patterns.add(Pattern.compile(Pattern.quote(badWord), Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE));
            }
            return patterns;
        } catch (IOException exception) {
            return List.of();
        }
    }

    private String toMask(java.util.regex.MatchResult matchResult) {
        int length = matchResult.group().length();
        if (length <= 0) {
            return matchResult.group();
        }
        return "*".repeat(length);
    }
}

