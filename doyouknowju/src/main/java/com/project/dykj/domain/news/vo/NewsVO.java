package com.project.dykj.domain.news.vo;

import lombok.Data;

@Data
public class NewsVO {
    private Long newsId;
    private String title;
    private String newsUrl;
    private String imageUrl;
    private String pubDate;
    private String aiSummary;
    private String newsCategory; // category에서 newsCategory로 변경
    private String createdAt;
}