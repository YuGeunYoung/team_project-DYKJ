package com.project.dykj.domain.news.controller;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.project.dykj.domain.news.service.NewsService;
import com.project.dykj.domain.news.vo.NewsVO;

@RestController
@RequestMapping("/api/news")
public class NewsController {

    @Autowired
    private NewsService newsService;

    @GetMapping
    public List<NewsVO> getNews() {
        return newsService.getLatestNews();
    }

    @PostMapping("/refresh")
    public String refresh() {
        newsService.refreshNews();
        return "뉴스 갱신 완료!";
    }
}