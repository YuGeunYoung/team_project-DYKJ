package com.project.dykj.domain.news.service;

import java.util.List;
import com.project.dykj.domain.news.vo.NewsVO;

public interface NewsService {
    List<NewsVO> getLatestNews();
    void refreshNews();
}